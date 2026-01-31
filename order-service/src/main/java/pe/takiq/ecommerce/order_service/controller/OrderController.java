package pe.takiq.ecommerce.order_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pe.takiq.ecommerce.order_service.dto.CartDTO;
import pe.takiq.ecommerce.order_service.model.Order;
import pe.takiq.ecommerce.order_service.service.OrderService;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Order> createPendingOrder(
            @RequestBody CartDTO cart,
            @RequestParam String guestId) {
        Order order = orderService.createPendingOrderFromCart(cart, guestId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{orderId}/confirm-payment")
    public ResponseEntity<Order> confirmPayment(
            @PathVariable String orderId,
            @RequestParam String paymentId) {
        Order confirmed = orderService.confirmPayment(orderId, paymentId);
        return ResponseEntity.ok(confirmed);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderForGuest(
            @PathVariable String orderId,
            @RequestParam String email) {
        Order order = orderService.getOrderForGuest(orderId, email);
        return ResponseEntity.ok(order);
    }
}