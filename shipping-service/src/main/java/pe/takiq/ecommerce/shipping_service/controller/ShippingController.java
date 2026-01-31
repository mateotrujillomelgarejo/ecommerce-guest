package pe.takiq.ecommerce.shipping_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.takiq.ecommerce.shipping_service.dto.ShippingCalculationRequest;
import pe.takiq.ecommerce.shipping_service.dto.ShippingCalculationResponse;
import pe.takiq.ecommerce.shipping_service.model.Shipment;
import pe.takiq.ecommerce.shipping_service.repository.ShipmentRepository;
import pe.takiq.ecommerce.shipping_service.service.ShippingService;

@RestController
@RequestMapping("/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;
    private final ShipmentRepository shipmentRepository;

    @PostMapping("/quote")
    public ResponseEntity<ShippingCalculationResponse> calculate(@RequestBody ShippingCalculationRequest request) {
        return ResponseEntity.ok(shippingService.calculateShipping(request));
    }

    // Para admin / seguimiento
    @GetMapping("/tracking/{orderId}")
    public ResponseEntity<Shipment> getShipmentByOrder(@PathVariable("orderId") String orderId) {
        return shipmentRepository.findByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}