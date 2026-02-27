package pe.takiq.ecommerce.pricing_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import pe.takiq.ecommerce.pricing_service.client.ProductPriceFetcher;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingService {

    private final ProductPriceFetcher productPriceFetcher; // NUEVO: Extraído para que el Circuit Breaker funcione
    private final CouponRepository couponRepository;
    private final PromotionRepository promotionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_HASH_KEY = "materialized_prices";
    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.18");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    public PriceCalculationResponse calculatePrice(PriceCalculationRequest request) {
        validateRequest(request);

        List<String> productIds = request.getItems().stream()
                .map(CartItem::getProductId)
                .collect(Collectors.toList());

        Map<String, BigDecimal> productPrices = getBulkProductPrices(productIds);

        BigDecimal subtotalBeforeDiscounts = ZERO;
        BigDecimal totalDiscount = ZERO;
        List<ItemPrice> detailedItems = new ArrayList<>();

        for (CartItem item : request.getItems()) {
            BigDecimal basePrice = productPrices.getOrDefault(item.getProductId(), ZERO);
            if (basePrice.equals(ZERO)) {
                log.warn("El producto {} no devolvió un precio válido. Se asume 0.", item.getProductId());
            }

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
            Optional<Coupon> couponOpt = getActiveCoupon(request.getCouponCode()); // OPTIMIZADO: Usa Caché
            
            if (couponOpt.isPresent()) {
                Coupon coupon = couponOpt.get();
                boolean isUsageValid = coupon.getMaxUses() == null || coupon.getUsesCount() < coupon.getMaxUses();
                
                if (isUsageValid) {
                    if (coupon.getDiscountPercent() != null) {
                        couponDiscount = subtotalBeforeDiscounts.multiply(coupon.getDiscountPercent());
                    } else if (coupon.getDiscountAmount() != null) {
                        couponDiscount = coupon.getDiscountAmount().min(subtotalBeforeDiscounts);
                    }
                    totalDiscount = totalDiscount.add(couponDiscount);
                    appliedCoupon = coupon.getCode();
                } else {
                    log.warn("Cupón {} alcanzó su límite máximo de usos.", coupon.getCode());
                }
            }
        }

        BigDecimal subtotalAfterDiscounts = subtotalBeforeDiscounts.subtract(totalDiscount);
        BigDecimal tax = subtotalAfterDiscounts.multiply(DEFAULT_TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal shipping = calculateShippingPlaceholder(subtotalAfterDiscounts); 

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

    protected Map<String, BigDecimal> getBulkProductPrices(List<String> productIds) {
        Map<String, BigDecimal> prices = new java.util.HashMap<>();
        List<String> missingInRedis = new java.util.ArrayList<>();

        // 1. Buscamos en la vista materializada de Redis
        for (String id : productIds) {
            Object cachedPrice = redisTemplate.opsForHash().get(REDIS_HASH_KEY, id);
            if (cachedPrice != null) {
                prices.put(id, new BigDecimal(cachedPrice.toString()));
            } else {
                missingInRedis.add(id);
            }
        }

        // 2. Solo los que faltan van al Product-Service (vía el nuevo Fetcher resiliente)
        if (!missingInRedis.isEmpty()) {
            List<ProductPriceDTO> fallbackPrices = productPriceFetcher.fetchPricesConResiliencia(missingInRedis);
            for (ProductPriceDTO dto : fallbackPrices) {
                prices.put(dto.getId(), dto.getPrice());
                redisTemplate.opsForHash().put(REDIS_HASH_KEY, dto.getId(), dto.getPrice().toString());
            }
        }
        return prices;
    }

    private BigDecimal applyProductPromotions(BigDecimal basePrice, String productId) {
        BigDecimal price = basePrice;
        List<Promotion> promos = getActivePromotions(productId); // OPTIMIZADO: Usa Caché
        for (Promotion promo : promos) {
            price = price.multiply(BigDecimal.ONE.subtract(promo.getDiscountPercent()));
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }


    @Cacheable(value = "promotions", key = "#productId")
    public List<Promotion> getActivePromotions(String productId) {
        return promotionRepository.findByProductIdAndActiveTrue(productId);
    }

    @Cacheable(value = "coupons", key = "#code")
    public Optional<Coupon> getActiveCoupon(String code) {
        return couponRepository.findByCodeAndActiveTrue(code);
    }

    @CacheEvict(value = {"promotions", "coupons"}, allEntries = true)
    public void invalidateCaches() {
        log.info("Reglas de precios (cupones/promociones) han sido modificadas. Cachés invalidadas.");
    }

    private BigDecimal calculateShippingPlaceholder(BigDecimal subtotal) {
        if (subtotal.compareTo(new BigDecimal("300")) >= 0) return ZERO;
        if (subtotal.compareTo(new BigDecimal("100")) >= 0) return new BigDecimal("10.00");
        return new BigDecimal("15.00");
    }

    private void validateRequest(PriceCalculationRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("El carrito debe tener al menos un producto");
        }
    }
}