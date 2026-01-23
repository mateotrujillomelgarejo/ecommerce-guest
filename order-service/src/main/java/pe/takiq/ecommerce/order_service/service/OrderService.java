package pe.takiq.ecommerce.order_service.service;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import pe.takiq.ecommerce.order_service.dto.CartDTO;
import pe.takiq.ecommerce.order_service.dto.CartItemDTO;
import pe.takiq.ecommerce.order_service.model.Order;
import pe.takiq.ecommerce.order_service.model.OrderItem;
import pe.takiq.ecommerce.order_service.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order createPendingOrderFromCart(CartDTO cart, String guestEmail) {
        Order order = new Order();
            order.setCartId(cart.getId());
            order.setGuestEmail(guestEmail);
            order.setStatus("PENDING_PAYMENT");
            order.setItems(new ArrayList<>());

        double total = 0.0;

        for (CartItemDTO cartItem : cart.getItems()) {
            OrderItem item = OrderItem.builder()
                    .productId(cartItem.getProductId())
                    .productName(cartItem.getProductName())
                    .unitPrice(cartItem.getPrice())
                    .quantity(cartItem.getQuantity())
                    .subtotal(cartItem.getPrice() * cartItem.getQuantity())
                    .order(order)
                    .build();
            
            order.getItems().add(item);
            total += item.getSubtotal();
        }


        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    @Transactional
    public Order confirmOrder(String orderId, String paymentId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new IllegalStateException("La orden no está pendiente de pago");
        }

        order.setStatus("CONFIRMED");
        order.setPaymentId(paymentId);
        order.setConfirmedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        return savedOrder;
    }

    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
    }
}