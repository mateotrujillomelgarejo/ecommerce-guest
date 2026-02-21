package pe.takiq.ecommerce.pricing_service.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import pe.takiq.ecommerce.pricing_service.client.ProductClient;
import pe.takiq.ecommerce.pricing_service.dto.*;
import pe.takiq.ecommerce.pricing_service.dto.PriceCalculationRequest.CartItem;
import pe.takiq.ecommerce.pricing_service.dto.PriceCalculationResponse.ItemPrice;
import pe.takiq.ecommerce.pricing_service.model.Coupon;
import pe.takiq.ecommerce.pricing_service.model.Promotion;
import pe.takiq.ecommerce.pricing_service.repository.CouponRepository;
import pe.takiq.ecommerce.pricing_service.repository.PromotionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingService {

    private final ProductClient productClient;
    private final CouponRepository couponRepository;
    private final PromotionRepository promotionRepository;

    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.18");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Cacheable("pricing")
    public PriceCalculationResponse calculatePrice(PriceCalculationRequest request) {
        validateRequest(request);

        BigDecimal subtotalBeforeDiscounts = ZERO;
        BigDecimal totalDiscount = ZERO;
        List<ItemPrice> detailedItems = new ArrayList<>();

        for (CartItem item : request.getItems()) {
            BigDecimal basePrice = getProductPrice(item.getProductId());
            BigDecimal effectiveUnitPrice = applyProductPromotions(basePrice, item.getProductId());

            BigDecimal originalLine = effectiveUnitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotalBeforeDiscounts = subtotalBeforeDiscounts.add(originalLine);

            detailedItems.add(new ItemPrice(
                    item.getProductId(),
                    basePrice,
                    effectiveUnitPrice,
                    item.getQuantity(),
                    originalLine
            ));
        }

        String appliedCoupon = null;
        BigDecimal couponDiscount = ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            Optional<Coupon> couponOpt = couponRepository.findByCodeAndActiveTrue(request.getCouponCode());
            if (couponOpt.isPresent()) {
                Coupon coupon = couponOpt.get();
                if (coupon.getDiscountPercent() != null) {
                    couponDiscount = subtotalBeforeDiscounts.multiply(coupon.getDiscountPercent());
                } else if (coupon.getDiscountAmount() != null) {
                    couponDiscount = coupon.getDiscountAmount().min(subtotalBeforeDiscounts);
                }
                totalDiscount = totalDiscount.add(couponDiscount);
                appliedCoupon = coupon.getCode();
                couponRepository.incrementUses(coupon.getCode());
            }
        }

        BigDecimal subtotalAfterDiscounts = subtotalBeforeDiscounts.subtract(totalDiscount);
        BigDecimal tax = subtotalAfterDiscounts.multiply(DEFAULT_TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shipping = calculateShippingPlaceholder(subtotalAfterDiscounts); // Futuro: call shipping-service

        BigDecimal grandTotal = subtotalAfterDiscounts.add(tax).add(shipping);

        PriceCalculationResponse response = new PriceCalculationResponse();
        response.setSubtotal(subtotalBeforeDiscounts);
        response.setDiscountAmount(totalDiscount);
        response.setTaxAmount(tax);
        response.setShippingAmount(shipping);
        response.setTotal(grandTotal);
        response.setItems(detailedItems);
        response.setAppliedCoupon(appliedCoupon);

        return response;
    }

    @CircuitBreaker(name = "productClient", fallbackMethod = "productPriceFallback")
    @Cacheable(value = "productPrices", key = "#productId")
    protected BigDecimal getProductPrice(String productId) {
        ProductPriceDTO dto = productClient.getProductPrice(productId);
        return dto.getPrice();
    }

    protected BigDecimal productPriceFallback(String productId, Throwable t) {
        log.error("Fallo crítico: No se pudo obtener el precio del producto {}.", productId, t);
        throw new RuntimeException("Servicio de catálogo de precios temporalmente no disponible. Intente en unos minutos.");
    }

    private BigDecimal applyProductPromotions(BigDecimal basePrice, String productId) {
        BigDecimal price = basePrice;
        List<Promotion> promos = promotionRepository.findByProductIdAndActiveTrue(productId);
        for (Promotion promo : promos) {
            price = price.multiply(BigDecimal.ONE.subtract(promo.getDiscountPercent()));
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateShippingPlaceholder(BigDecimal subtotal) {
        // Placeholder simple (futuro: Feign a shipping-service)
        if (subtotal.compareTo(new BigDecimal("300")) >= 0) return ZERO;
        if (subtotal.compareTo(new BigDecimal("100")) >= 0) return new BigDecimal("10.00");
        return new BigDecimal("15.00");
    }

    private void validateRequest(PriceCalculationRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("El carrito debe tener al menos un producto");
        }
    }

    // Para invalidar cuando admin crea/actualiza promoción o cupón
    @CacheEvict(value = {"productPrices", "priceCalculations"}, allEntries = true)
    public void invalidateCaches() {
        log.info("Invalidando cachés de precios tras cambio en reglas");
    }
}