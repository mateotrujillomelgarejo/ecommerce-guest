package pe.takiq.ecommerce.pricing_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PricingService {

    private final ProductPriceFetcher productPriceFetcher;
    private final CouponRepository couponRepository;
    private final PromotionRepository promotionRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_HASH_KEY  = "materialized_prices";
    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.18");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Transactional
    public PriceCalculationResponse calculatePrice(PriceCalculationRequest request) {
        validateRequest(request);

        List<String> productIds = request.getItems().stream()
                .map(CartItem::getProductId)
                .collect(Collectors.toList());

        Map<String, BigDecimal> productPrices = getBulkProductPrices(productIds);

        BigDecimal subtotalBeforeDiscounts = ZERO;
        BigDecimal totalDiscount           = ZERO;
        List<ItemPrice> detailedItems      = new ArrayList<>();

        for (CartItem item : request.getItems()) {
            BigDecimal basePrice = productPrices.getOrDefault(item.getProductId(), ZERO);
            if (basePrice.compareTo(ZERO) == 0) {
                log.warn("Producto {} sin precio válido. Se usa 0.", item.getProductId());
            }

            BigDecimal effectiveUnitPrice = applyProductPromotions(basePrice, item.getProductId());
            BigDecimal lineTotal = effectiveUnitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            subtotalBeforeDiscounts = subtotalBeforeDiscounts.add(lineTotal);

            detailedItems.add(new ItemPrice(
                    item.getProductId(),
                    basePrice,
                    effectiveUnitPrice,
                    item.getQuantity(),
                    lineTotal
            ));
        }

        String appliedCoupon  = null;
        BigDecimal couponDiscount = ZERO;

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            Optional<Coupon> couponOpt = getActiveCoupon(request.getCouponCode());

            if (couponOpt.isPresent()) {
                Coupon coupon = couponOpt.get();
                boolean withinLimit = coupon.getMaxUses() == null
                        || coupon.getUsesCount() < coupon.getMaxUses();

                if (withinLimit) {
                    if (coupon.getDiscountPercent() != null) {
                        couponDiscount = subtotalBeforeDiscounts
                                .multiply(coupon.getDiscountPercent())
                                .setScale(2, RoundingMode.HALF_UP);
                    } else if (coupon.getDiscountAmount() != null) {
                        // El descuento nunca supera el subtotal
                        couponDiscount = coupon.getDiscountAmount()
                                .min(subtotalBeforeDiscounts);
                    }
                    totalDiscount = totalDiscount.add(couponDiscount);
                    appliedCoupon = coupon.getCode();

                    // ✅ Incrementar usesCount de forma atómica con optimistic locking
                    // El @Modifying + @Query hace UPDATE directo en DB sin pasar por la caché
                    int updated = couponRepository.incrementUses(coupon.getCode());
                    if (updated == 0) {
                        log.warn("No se pudo incrementar usesCount para cupón {}", coupon.getCode());
                    }

                    // Invalidar la caché del cupón — el usesCount cambió
                    invalidateCouponCache(coupon.getCode());

                } else {
                    log.warn("Cupón {} alcanzó su límite de usos.", coupon.getCode());
                }
            }
        }

        BigDecimal subtotalAfterDiscounts = subtotalBeforeDiscounts.subtract(totalDiscount)
                .max(ZERO); // nunca negativo
        BigDecimal tax      = subtotalAfterDiscounts.multiply(DEFAULT_TAX_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal shipping = calculateShipping(subtotalAfterDiscounts);
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

    /**
     * Obtiene precios en bloque usando HMGET — una sola roundtrip a Redis para todos los IDs.
     * Los IDs faltantes se piden en bloque a Product Service en una sola llamada.
     * Nunca hace N llamadas individuales a Redis ni a Product Service.
     */
    protected Map<String, BigDecimal> getBulkProductPrices(List<String> productIds) {
        Map<String, BigDecimal> prices = new HashMap<>();

        // ✅ HMGET: una sola roundtrip a Redis para todos los IDs
        List<Object> cachedValues = redisTemplate.opsForHash()
                .multiGet(REDIS_HASH_KEY, new ArrayList<>(productIds));

        List<String> missingIds = new ArrayList<>();

        for (int i = 0; i < productIds.size(); i++) {
            Object cached = cachedValues.get(i);
            if (cached != null) {
                prices.put(productIds.get(i), new BigDecimal(cached.toString()));
            } else {
                missingIds.add(productIds.get(i));
            }
        }

        // ✅ Una sola llamada a Product Service para todos los faltantes
        if (!missingIds.isEmpty()) {
            log.debug("Precios no encontrados en Redis para {} productos, consultando Product Service",
                    missingIds.size());
            List<ProductPriceDTO> fromProduct =
                    productPriceFetcher.fetchPricesConResiliencia(missingIds);

            for (ProductPriceDTO dto : fromProduct) {
                prices.put(dto.getId(), dto.getPrice());
                // Guardar en el Hash de Redis para futuras consultas
                redisTemplate.opsForHash().put(
                        REDIS_HASH_KEY, dto.getId(), dto.getPrice().toString());
            }
        }

        return prices;
    }

    private BigDecimal applyProductPromotions(BigDecimal basePrice, String productId) {
        BigDecimal price = basePrice;
        List<Promotion> promos = getActivePromotions(productId);
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

    @CacheEvict(value = "coupons", key = "#code")
    public void invalidateCouponCache(String code) {
        log.debug("Caché de cupón invalidada: {}", code);
    }

    @CacheEvict(value = {"promotions", "coupons"}, allEntries = true)
    public void invalidateAllCaches() {
        log.info("Cachés de cupones y promociones invalidadas completamente.");
    }

    private BigDecimal calculateShipping(BigDecimal subtotal) {
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