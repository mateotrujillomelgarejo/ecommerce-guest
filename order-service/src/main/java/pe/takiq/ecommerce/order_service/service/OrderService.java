package pe.takiq.ecommerce.order_service.service;

import lombok.RequiredArgsConstructor;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import pe.takiq.ecommerce.order_service.client.CustomerClient;
import pe.takiq.ecommerce.order_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.order_service.dto.CartDTO;
import pe.takiq.ecommerce.order_service.dto.CartItemDTO;
import pe.takiq.ecommerce.order_service.dto.CreateOrderRequest;
import pe.takiq.ecommerce.order_service.dto.GuestResponseDTO;
import pe.takiq.ecommerce.order_service.dto.OrderResponseDTO;
import pe.takiq.ecommerce.order_service.event.OrderCreatedEvent;
import pe.takiq.ecommerce.order_service.model.Order;
import pe.takiq.ecommerce.order_service.model.Order.OrderItem;
import pe.takiq.ecommerce.order_service.model.OrderStatus;
import pe.takiq.ecommerce.order_service.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final CustomerClient customerClient;

    @Transactional
    public OrderResponseDTO createOrderAfterPayment(CreateOrderRequest request) {
        
        // Validación básica
        if (request.getPaymentId() == null || request.getTotal() == null || request.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("paymentId y total son obligatorios");
        }
        if (request.getGuestId() == null) {
            throw new IllegalArgumentException("guestId es obligatorio");
        }
        if (request.getSessionId() == null) {
            throw new IllegalArgumentException("sessionId es obligatorio");
        }

        Order order = new Order();
        order.setGuestId(request.getGuestId());
        order.setTotalAmount(request.getTotal());
        order.setPaymentId(request.getPaymentId());
        order.setStatus(OrderStatus.PAID);

        // Mapear items SOLO si existen
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<OrderItem> orderItems = request.getItems().stream()
                .map(dto -> {
                    OrderItem item = new OrderItem();
                    item.setProductId(dto.getProductId());
                    item.setQuantity(dto.getQuantity());
                    item.setPrice(dto.getPrice());
                    return item;
                })
                .collect(Collectors.toList());
            order.setItems(orderItems);
        } else {
            // Opcional: si quieres permitir órdenes sin items, dejar null o lista vacía
            order.setItems(new ArrayList<>()); // o dejarlo null si tu modelo lo permite
        }

        order = repository.save(order);

        // Obtener guestEmail
        String guestEmail;
        // Opción A: Agregar guestEmail al CreateOrderRequest y usarlo directamente
        // guestEmail = request.getGuestEmail(); // Asumiendo que lo agregas al DTO

        // Opción B: Consultar customer-service con guestId o sessionId (agrega CustomerClient)
        GuestResponseDTO guest = customerClient.getGuestBySessionId(request.getSessionId());
        guestEmail = guest.getEmail();

        // Construir OrderCreatedEvent COMPLETO
        OrderCreatedEvent event = OrderCreatedEvent.builder()
            .orderId(order.getId())
            .guestId(order.getGuestId())
            .sessionId(request.getSessionId())         // ← CRÍTICO: agregar esto
            .guestEmail(guestEmail)                    // ← CRÍTICO: agregar esto
            .subtotal(request.getTotal())              // o calcular si tienes subtotal separado
            .discount(BigDecimal.ZERO)                 // Ajusta si tienes discounts
            .tax(BigDecimal.ZERO)                      // Ajusta si tienes taxes
            .shippingCost(request.getShippingCost() != null ? request.getShippingCost() : BigDecimal.ZERO)
            .total(order.getTotalAmount())
            .items(
                order.getItems() != null ?
                    order.getItems().stream()
                        .map(item -> OrderCreatedEvent.OrderItemEvent.builder()
                            .productId(item.getProductId())
                            .quantity(item.getQuantity())
                            .unitPriceSnapshot(BigDecimal.valueOf(item.getPrice() != null ? item.getPrice() : 0.0))
                            .build()
                        )
                        .collect(Collectors.toList())
                    : List.of()
            )
            .createdAt(order.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant())
            .build();

        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE,
            "order.created",
            event
        );

        // Respuesta
        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderId(order.getId());
        response.setGuestId(order.getGuestId());
        response.setTotalAmount(order.getTotalAmount());
        response.setPaymentId(order.getPaymentId());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        // Si quieres incluir items en la respuesta también puedes mapearlos aquí
        if (order.getItems() != null) {
            response.setItems(order.getItems().stream()
                .map(oi -> {
                    CartItemDTO dto = new CartItemDTO();
                    dto.setProductId(oi.getProductId());
                    dto.setQuantity(oi.getQuantity());
                    dto.setPrice(oi.getPrice());
                    return dto;
                })
                .collect(Collectors.toList()));
        }

        return response;
    }

    //por ahora este se dejara de lado y no se utilizara
    public OrderResponseDTO createOrder(CartDTO cartDTO) {
        Order order = new Order();
        order.setGuestId(cartDTO.getGuestId());
        order.setTotalAmount(cartDTO.getTotal());
        order.setItems(cartDTO.getItems().stream()
                .map(item -> {
                    OrderItem oi = new OrderItem();
                    oi.setProductId(item.getProductId());
                    oi.setQuantity(item.getQuantity());
                    oi.setPrice(item.getPrice());
                    return oi;
                }).collect(Collectors.toList()));

        order = repository.save(order);

        OrderResponseDTO response = new OrderResponseDTO();
        response.setOrderId(order.getId());
        response.setGuestId(order.getGuestId());
        response.setItems(cartDTO.getItems());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());

        return response;
    }

    public OrderResponseDTO getOrder(String id) {
        Order order = repository.findById(id).orElseThrow();
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setOrderId(order.getId());
        dto.setGuestId(order.getGuestId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPaymentId(order.getPaymentId());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());
        return dto;
    }

    public void updateStatus(String orderId, OrderStatus newStatus) {
        Order order = repository.findById(orderId).orElseThrow();
        order.setStatus(newStatus);
        repository.save(order);
    }
}