package pe.takiq.ecommerce.order_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.takiq.ecommerce.order_service.dto.CreateOrderRequest;
import pe.takiq.ecommerce.order_service.dto.CreatePendingOrderRequest;
import pe.takiq.ecommerce.order_service.dto.OrderResponseDTO;
import pe.takiq.ecommerce.order_service.service.OrderService;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody CreateOrderRequest request) {
        OrderResponseDTO response = orderService.createOrderAfterPayment(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/pending")
    public ResponseEntity<OrderResponseDTO> createPending(@RequestBody CreatePendingOrderRequest req) {
        return ResponseEntity.ok(orderService.createPendingOrder(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrder(@PathVariable("id") String id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }
}