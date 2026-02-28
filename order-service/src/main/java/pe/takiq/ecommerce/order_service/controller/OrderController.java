package pe.takiq.ecommerce.order_service.controller;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.takiq.ecommerce.order_service.dto.CreatePendingOrderRequest;
import pe.takiq.ecommerce.order_service.dto.OrderResponseDTO;
import pe.takiq.ecommerce.order_service.model.OrderStatus;
import pe.takiq.ecommerce.order_service.service.OrderService;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/pending")
    public ResponseEntity<OrderResponseDTO> createPending(@RequestBody CreatePendingOrderRequest req) {
        return ResponseEntity.ok(orderService.createPendingOrder(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrder(@PathVariable("id") String id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }


    @GetMapping("/all")
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrdersForAdmin());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateOrderStatus(
            @PathVariable("id") String id, 
            @RequestParam("status") OrderStatus status) {
        orderService.updateStatus(id, status);
        return ResponseEntity.ok().build();
    }
}