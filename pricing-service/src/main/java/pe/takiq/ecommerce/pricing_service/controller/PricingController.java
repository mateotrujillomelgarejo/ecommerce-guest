package pe.takiq.ecommerce.pricing_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.takiq.ecommerce.pricing_service.dto.PriceCalculationRequest;
import pe.takiq.ecommerce.pricing_service.dto.PriceCalculationResponse;
import pe.takiq.ecommerce.pricing_service.model.Coupon;
import pe.takiq.ecommerce.pricing_service.model.Promotion;
import pe.takiq.ecommerce.pricing_service.repository.CouponRepository;
import pe.takiq.ecommerce.pricing_service.repository.PromotionRepository;
import pe.takiq.ecommerce.pricing_service.service.PricingService;

import java.util.List;

@RestController
@RequestMapping("/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;
    private final CouponRepository couponRepository;
    private final PromotionRepository promotionRepository;

    @PostMapping("/calculate")
    public ResponseEntity<PriceCalculationResponse> calculate(@RequestBody PriceCalculationRequest request) {
        return ResponseEntity.ok(pricingService.calculatePrice(request));
    }

    // ─── Admin Endpoints (futuro protegidos con security) ───

    @PostMapping("/admin/coupons")
    public ResponseEntity<Coupon> createCoupon(@RequestBody Coupon coupon) {
        Coupon saved = couponRepository.save(coupon);
        pricingService.invalidateCaches();
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/admin/coupons")
    public ResponseEntity<List<Coupon>> getAllCoupons() {
        return ResponseEntity.ok(couponRepository.findAll());
    }

    @PutMapping("/admin/coupons/{id}")
    public ResponseEntity<Coupon> updateCoupon(@PathVariable Long id, @RequestBody Coupon coupon) {
        coupon.setId(id);
        Coupon updated = couponRepository.save(coupon);
        pricingService.invalidateCaches();
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/admin/promotions")
    public ResponseEntity<Promotion> createPromotion(@RequestBody Promotion promotion) {
        Promotion saved = promotionRepository.save(promotion);
        pricingService.invalidateCaches();
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/admin/promotions")
    public ResponseEntity<List<Promotion>> getAllPromotions() {
        return ResponseEntity.ok(promotionRepository.findAll());
    }
}