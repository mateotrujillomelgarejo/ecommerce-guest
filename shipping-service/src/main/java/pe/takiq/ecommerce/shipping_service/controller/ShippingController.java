package pe.takiq.ecommerce.shipping_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import pe.takiq.ecommerce.shipping_service.dto.ShippingCalculationRequest;
import pe.takiq.ecommerce.shipping_service.dto.ShippingCalculationResponse;
import pe.takiq.ecommerce.shipping_service.model.Shipment;
import pe.takiq.ecommerce.shipping_service.repository.ShipmentRepository;
import pe.takiq.ecommerce.shipping_service.service.ShippingService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService service;
    private final ShipmentRepository repo;

    @GetMapping("/calculate")
    public ResponseEntity<ShippingCalculationResponse> calculate(
            @RequestParam(value = "orderTotal", defaultValue = "0") BigDecimal orderTotal) {
        ShippingCalculationRequest req = new ShippingCalculationRequest();
        req.setOrderTotal(orderTotal);
        return ResponseEntity.ok(service.calculateShipping(req));
    }

    @PostMapping("/quote")
    public ResponseEntity<ShippingCalculationResponse> quote(
            @RequestBody ShippingCalculationRequest req) {
        return ResponseEntity.ok(service.calculateShipping(req));
    }

    @GetMapping("/tracking/{orderId}")
    public ResponseEntity<Shipment> tracking(
            @PathVariable("orderId") String orderId) {
        return repo.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}