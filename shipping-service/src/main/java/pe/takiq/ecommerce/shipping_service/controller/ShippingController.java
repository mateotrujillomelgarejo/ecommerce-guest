package pe.takiq.ecommerce.shipping_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pe.takiq.ecommerce.shipping_service.dto.ShippingCalculationRequest;
import pe.takiq.ecommerce.shipping_service.dto.ShippingCalculationResponse;
import pe.takiq.ecommerce.shipping_service.model.Shipment;
import pe.takiq.ecommerce.shipping_service.repository.ShipmentRepository;
import pe.takiq.ecommerce.shipping_service.service.ShippingService;

@RestController
@RequestMapping("/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService service;
    private final ShipmentRepository repo;

    @PostMapping("/quote")
    public ResponseEntity<ShippingCalculationResponse> quote(
            @RequestBody ShippingCalculationRequest req) {
        return ResponseEntity.ok(service.calculateShipping(req));
    }

    @GetMapping("/tracking/{orderId}")
    public ResponseEntity<Shipment> tracking(@PathVariable("orderId") String orderId) {
        return repo.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
