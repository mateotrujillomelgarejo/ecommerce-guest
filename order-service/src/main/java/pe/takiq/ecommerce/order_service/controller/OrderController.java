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
            @RequestParam(name = "guestId", required = true) String guestId) {

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(null);
        }

        Order order = orderService.createPendingOrderFromCart(cart, guestId);
        return ResponseEntity.ok(order);
    }


    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<Order> confirmOrder(
            @PathVariable("orderId") String orderId,
            @RequestParam(name = "paymentId") String paymentId) {
        Order confirmedOrder = orderService.confirmOrder(orderId, paymentId);
        return ResponseEntity.ok(confirmedOrder);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(@PathVariable("orderId") String orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }
}