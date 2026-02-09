package pe.takiq.ecommerce.order_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.takiq.ecommerce.order_service.dto.CartDTO;
import pe.takiq.ecommerce.order_service.dto.OrderResponseDTO;
import pe.takiq.ecommerce.order_service.model.Order;
import pe.takiq.ecommerce.order_service.model.Order.OrderItem;
import pe.takiq.ecommerce.order_service.model.OrderStatus;
import pe.takiq.ecommerce.order_service.repository.OrderRepository;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;

    // Crea orden preliminar en PAYMENT_PENDING, NO publica eventos
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
        // Mapear items si es necesario
        return dto;
    }

    public void updateStatus(String orderId, OrderStatus newStatus) {
        Order order = repository.findById(orderId).orElseThrow();
        order.setStatus(newStatus);
        repository.save(order);
    }
}