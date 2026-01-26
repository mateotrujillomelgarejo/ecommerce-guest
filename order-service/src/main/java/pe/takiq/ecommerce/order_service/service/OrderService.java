package pe.takiq.ecommerce.order_service.service;

import lombok.RequiredArgsConstructor;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import pe.takiq.ecommerce.order_service.client.CustomerClient;
import pe.takiq.ecommerce.order_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.order_service.dto.CartDTO;
import pe.takiq.ecommerce.order_service.dto.CartItemDTO;
import pe.takiq.ecommerce.order_service.dto.CustomerDTO;
import pe.takiq.ecommerce.events.OrderConfirmedEvent;
import pe.takiq.ecommerce.order_service.model.Order;
import pe.takiq.ecommerce.order_service.model.OrderItem;
import pe.takiq.ecommerce.order_service.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final CustomerClient customerClient;

    @Transactional
    @CircuitBreaker(
        name = "customerClient",
        fallbackMethod = "customerFallback"
    )
    @Retry(name = "customerClient")
    public Order createPendingOrderFromCart(CartDTO cart, String guestId) {

        CustomerDTO customer = customerClient.getGuest(guestId);

        Order order = new Order();
        order.setCartId(cart.getId());
        order.setGuestEmail(customer.getEmail());
        order.setGuestId(guestId);
        order.setStatus("PENDING_PAYMENT");
        order.setItems(new ArrayList<>());

        double total = 0.0;

        for (CartItemDTO itemDto : cart.getItems()) {
            OrderItem item = OrderItem.builder()
                    .productId(itemDto.getProductId())
                    .productName(itemDto.getProductName())
                    .unitPrice(itemDto.getPrice())
                    .quantity(itemDto.getQuantity())
                    .subtotal(itemDto.getPrice() * itemDto.getQuantity())
                    .order(order)
                    .build();

            order.getItems().add(item);
            total += item.getSubtotal();
        }

        order.setTotalAmount(total);
        return orderRepository.save(order);
    }

    public Order customerFallback(
        CartDTO cart,
        String guestId,
        Throwable ex
    ) {
        throw new IllegalStateException(
            "Customer Service no disponible. No se puede crear la orden.",
            ex
        );
    }

@Transactional
public Order confirmOrder(String orderId, String paymentId) {

    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalStateException("Orden no encontrada"));

    if (!"PENDING_PAYMENT".equals(order.getStatus())) {
        throw new IllegalStateException("La orden no está pendiente de pago");
    }

        order.setStatus("CONFIRMED");
        order.setPaymentId(paymentId);
        order.setConfirmedAt(LocalDateTime.now());

            Order savedOrder = orderRepository.save(order);

        List<OrderConfirmedEvent.OrderItemEvent> eventItems =
                    savedOrder.getItems().stream()
                        .map(item -> new OrderConfirmedEvent.OrderItemEvent(
                                item.getProductId(),
                                item.getQuantity()
                            ))
                            .collect(Collectors.toList());

            OrderConfirmedEvent event = new OrderConfirmedEvent(
                    savedOrder.getId(),
                    savedOrder.getCartId(),
                    savedOrder.getGuestEmail(),
                    savedOrder.getTotalAmount(),
                    savedOrder.getPaymentId(),
                savedOrder.getConfirmedAt(),
                savedOrder.getStatus(),
                    eventItems
        );

            rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EVENTS_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_CONFIRMED,
                event
            );

            System.out.printf("OrderConfirmedEvent enviado. orderId={}", savedOrder.getId());

            return savedOrder;
        }


    public Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));
    }
}