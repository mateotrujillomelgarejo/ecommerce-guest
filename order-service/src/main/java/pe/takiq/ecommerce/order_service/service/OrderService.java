package pe.takiq.ecommerce.order_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import pe.takiq.ecommerce.order_service.client.CustomerClient;
import pe.takiq.ecommerce.order_service.client.PricingClient;
import pe.takiq.ecommerce.order_service.client.ShippingClient;
import pe.takiq.ecommerce.order_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.order_service.dto.CartDTO;
import pe.takiq.ecommerce.order_service.dto.CartItemDTO;
import pe.takiq.ecommerce.order_service.dto.CustomerDTO;
import pe.takiq.ecommerce.order_service.dto.request.PriceCalculationRequest;
import pe.takiq.ecommerce.order_service.dto.response.PriceCalculationResponse;
import pe.takiq.ecommerce.events.OrderConfirmedEvent;
import pe.takiq.ecommerce.events.OrderCreatedEvent;
import pe.takiq.ecommerce.events.OrderShippedEvent;
import pe.takiq.ecommerce.order_service.model.Order;
import pe.takiq.ecommerce.order_service.model.OrderItem;
import pe.takiq.ecommerce.order_service.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final CustomerClient customerClient;
    private final PricingClient pricingClient;     // opcional – snapshot
    private final ShippingClient shippingClient;   // opcional – snapshot

    // ────────────────────────────────────────────────
    //  Flujo 1: Crear orden pendiente (pre-pago)
    // ────────────────────────────────────────────────
    @Transactional
    public Order createPendingOrderFromCart(CartDTO cart, String guestId) {

        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Carrito vacío");
        }

        // Validar que el guest existe y recuperar email
        CustomerDTO guest = validateAndGetGuest(guestId);

        Order order = new Order();
        order.setGuestId(guestId);
        order.setEmail(guest.getEmail());
        order.setSessionId(cart.getId());
        order.setStatus(Order.OrderStatus.PAYMENT_PENDING);

        // Mapear items
        List<OrderItem> items = cart.getItems().stream()
                .map(i -> OrderItem.builder()
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .unitPrice(i.getPrice())
                        .quantity(i.getQuantity())
                        .subtotal(i.getPrice() * i.getQuantity())
                        .order(order)
                        .build())
                .collect(Collectors.toList());

        order.setItems(items);

        // Subtotal básico (se puede revalidar después con pricing si quieres)
        double subtotal = items.stream().mapToDouble(OrderItem::getSubtotal).sum();
        order.setSubtotal(BigDecimal.valueOf(subtotal));
        order.setTotalAmount(BigDecimal.valueOf(subtotal)); // se ajustará después

        Order saved = repository.save(order);

        // Emitir evento inmediatamente (inventory, shipping, notification lo escucharán)
        publishOrderCreated(saved);

        return saved;
    }

    // ────────────────────────────────────────────────
    //  Flujo 2: Confirmación de pago (debe ser idempotente)
    // ────────────────────────────────────────────────
    @Transactional
    public Order confirmPayment(String orderId, String paymentId) {

        // Idempotencia #1: ya fue confirmado con este paymentId
        Optional<Order> existing = repository.findByPaymentId(paymentId);
        if (existing.isPresent()) {
            log.info("Orden ya confirmada previamente con paymentId: {}", paymentId);
            return existing.get();
        }

        Order order = repository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + orderId));

        if (order.getStatus() != Order.OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("La orden no está en estado pendiente de pago");
        }

        // Aquí idealmente validarías con payment-service que el pago es SUCCESS
        // pero como ya lo tienes en el flujo, asumimos que este endpoint solo se llama tras éxito

        order.transitionTo(Order.OrderStatus.PAID, null);
        order.setPaymentId(paymentId);

        Order saved = repository.save(order);

        // Emitir evento de confirmación → notification, inventory (si no lo hizo antes), etc.
        publishOrderConfirmed(saved);

        return saved;
    }

    // ────────────────────────────────────────────────
    //  Consulta segura para guest (paso 10 del flujo)
    // ────────────────────────────────────────────────
    public Order getOrderForGuest(String orderId, String email) {
        return repository.findByIdAndEmail(orderId, email)
                .orElseThrow(() -> new SecurityException("Orden no encontrada o no autorizada"));
    }

    // ────────────────────────────────────────────────
    //  Listeners recomendados (agregar en esta clase o en @Component separado)
    // ────────────────────────────────────────────────

    @RabbitListener(queues = "order.shipped.queue")
    @Transactional
    public void onOrderShipped(OrderShippedEvent event) {
        Order order = repository.findById(event.getOrderId())
                .orElse(null);

        if (order == null || order.getStatus().ordinal() >= Order.OrderStatus.SHIPPED.ordinal()) {
            return; // ya procesado o no existe
        }

        order.transitionTo(Order.OrderStatus.SHIPPED, null);
        order.setShippingId(event.getTrackingNumber());
        repository.save(order);

        log.info("Orden {} marcada como SHIPPED", order.getId());
    }

    @RabbitListener(queues = "order.delivered.queue")   // si shipping lo emite
    public void onOrderDelivered(OrderShippedEvent event) {
        // similar...
    }

    // ────────────────────────────────────────────────
    //  Helpers
    // ────────────────────────────────────────────────
    private CustomerDTO validateAndGetGuest(String guestId) {
        try {
            return customerClient.getGuest(guestId);
        } catch (FeignException e) {
            throw new IllegalArgumentException("Guest no válido o no encontrado: " + guestId, e);
        }
    }

private void publishOrderCreated(Order order) {

    var event = OrderCreatedEvent.builder()
            .orderId(order.getId())
            .cartId(order.getSessionId())                 // ✔ cartId correcto
            .guestEmail(order.getEmail())                 // ✔ email correcto
            .totalAmount(order.getTotalAmount().doubleValue())
            .status(order.getStatus().name())             // ✔ enum → string
            .createdAt(order.getCreatedAt())              // ✔ timestamp
            .items(order.getItems().stream()
                    .map(i -> new OrderCreatedEvent.OrderItemEvent(
                            i.getProductId(),
                            i.getQuantity()
                    ))
                    .collect(Collectors.toList())
            )
            .build();

    rabbitTemplate.convertAndSend(
            RabbitMQConfig.ORDER_EVENTS_EXCHANGE,
            RabbitMQConfig.ROUTING_KEY_CREATED,
            event
    );
}


    private void publishOrderConfirmed(Order order) {
        var event = OrderConfirmedEvent.builder()
                .orderId(order.getId())
                .paymentId(order.getPaymentId())
                .totalAmount(order.getTotalAmount().doubleValue())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EVENTS_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_CONFIRMED,
                event
        );
    }
}