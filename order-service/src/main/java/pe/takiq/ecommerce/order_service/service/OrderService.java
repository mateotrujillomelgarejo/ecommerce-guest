package pe.takiq.ecommerce.order_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Cambio a Transaccional de Spring

import feign.FeignException;

import pe.takiq.ecommerce.order_service.client.CustomerClient;
import pe.takiq.ecommerce.order_service.client.InventoryClient;
import pe.takiq.ecommerce.order_service.dto.*;
import pe.takiq.ecommerce.order_service.exception.BusinessException;
import pe.takiq.ecommerce.order_service.model.Order;
import pe.takiq.ecommerce.order_service.model.Order.OrderItem;
import pe.takiq.ecommerce.order_service.model.OrderStatus;
import pe.takiq.ecommerce.order_service.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;
    private final CustomerClient customerClient;
    private final InventoryClient inventoryClient;

    @Transactional
    public OrderResponseDTO createPendingOrder(CreatePendingOrderRequest request) {
        if (request.getTotal() == null || request.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El total es obligatorio y debe ser mayor a 0");
        }

        GuestResponseDTO guest;
        try {
            guest = customerClient.getGuestBySessionId(request.getSessionId());
        } catch (Exception e) {
            log.error("Error al comunicar con Customer Service para sessionId: {}", request.getSessionId(), e);
            throw new BusinessException("No pudimos validar tu sesión de usuario. Por favor, intenta nuevamente.");
        }

        String orderId = UUID.randomUUID().toString();

        ReserveStockRequest reserveReq = new ReserveStockRequest();
        reserveReq.setOrderId(orderId);
        List<ReserveStockRequest.Item> reserveItems = new ArrayList<>();
        if (request.getItems() != null) {
            for (CartItemDTO dto : request.getItems()) {
                ReserveStockRequest.Item i = new ReserveStockRequest.Item();
                i.setProductId(dto.getProductId());
                i.setQuantity(dto.getQuantity());
                reserveItems.add(i);
            }
        }
        reserveReq.setItems(reserveItems);
        
        try{
            inventoryClient.reserveStock(reserveReq);
        } catch (FeignException.BadRequest | FeignException.Conflict e) {
            throw new BusinessException("Stock insuficiente para uno o más productos. Por favor revisa tu carrito.");
        } catch (Exception e){
            log.error("Fallo del servicio de inventario", e);
            throw new BusinessException("No pudimos procesar el inventario en este momento. Intenta de nuevo más tarde.");
        }
        
        Order order = new Order();
        order.setId(orderId);
        order.setGuestId(request.getGuestId());
        order.setSessionId(request.getSessionId());
        order.setGuestEmail(guest.getEmail());
        order.setTotalAmount(request.getTotal());
        order.setStatus(OrderStatus.PAYMENT_PENDING);

        if (request.getItems() != null) {
            order.setItems(request.getItems().stream().map(dto -> {
                OrderItem item = new OrderItem();
                item.setProductId(dto.getProductId());
                item.setQuantity(dto.getQuantity());
                item.setPrice(dto.getPrice());
                return item;
            }).collect(Collectors.toList()));
        } else {
            order.setItems(new ArrayList<>());
        }

        order = repository.save(order);
        return mapToDTO(order);
    }

    public OrderResponseDTO getOrder(String id) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Orden no encontrada"));
        return mapToDTO(order);
    }

    @Transactional
    public void updateStatus(String orderId, OrderStatus newStatus) {
        Order order = repository.findById(orderId).orElseThrow();
        order.setStatus(newStatus);
        repository.save(order);
    }

    @Transactional
    public void markAsShipped(String orderId, String trackingNumber) {
        Order order = repository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.SHIPPED);
        order.setTrackingNumber(trackingNumber);
        repository.save(order);
    }

    @Transactional
    public void markAsFailed(String orderId, String reason) {
        Order order = repository.findById(orderId).orElseThrow();
        order.setStatus(OrderStatus.FAILED);
        order.setFailureReason(reason);
        repository.save(order);
    }

    private OrderResponseDTO mapToDTO(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setOrderId(order.getId());
        dto.setGuestId(order.getGuestId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPaymentId(order.getPaymentId());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setTrackingNumber(order.getTrackingNumber());
        
        if (order.getItems() != null) {
            dto.setItems(order.getItems().stream().map(item -> {
                CartItemDTO c = new CartItemDTO();
                c.setProductId(item.getProductId());
                c.setQuantity(item.getQuantity());
                c.setPrice(item.getPrice());
                return c;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}