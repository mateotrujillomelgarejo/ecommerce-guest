package pe.takiq.ecommerce.shipping_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.takiq.ecommerce.events.OrderCreatedEvent;
import pe.takiq.ecommerce.events.OrderShippedEvent;
import pe.takiq.ecommerce.shipping_service.client.CustomerClient;
import pe.takiq.ecommerce.shipping_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.shipping_service.dto.GuestResponseDTO;
import pe.takiq.ecommerce.shipping_service.dto.ShippingCalculationRequest;
import pe.takiq.ecommerce.shipping_service.dto.ShippingCalculationResponse;
import pe.takiq.ecommerce.shipping_service.model.Shipment;
import pe.takiq.ecommerce.shipping_service.repository.ShipmentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingService {

    private final ShipmentRepository shipmentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final CustomerClient customerClient;

    // Quote síncrono (paso 5 del flujo)
    public ShippingCalculationResponse calculateShipping(ShippingCalculationRequest request) {
        BigDecimal orderTotal = request.getOrderTotal() != null ? request.getOrderTotal() : BigDecimal.ZERO;

        BigDecimal cost = BigDecimal.ZERO;
        String message = "Envío estándar a todo el Perú";

        if (orderTotal.compareTo(new BigDecimal("300")) >= 0) {
            cost = BigDecimal.ZERO;
            message = "¡Envío gratis por superar S/ 300!";
        } else if (orderTotal.compareTo(new BigDecimal("100")) >= 0) {
            cost = new BigDecimal("10.00");
        } else {
            cost = new BigDecimal("15.00");
        }

        return new ShippingCalculationResponse(cost, "3-5 días hábiles", message);
    }

    @Transactional
    public void createShipmentFromOrder(OrderCreatedEvent event) {
    GuestResponseDTO guest = customerClient.getGuest(event.getGuestId());

    // 2. Usar los datos reales
    Shipment shipment = Shipment.builder()
            .orderId(event.getOrderId())
            .postalCode(guest.getAddress() != null ? guest.getAddress().getPostalCode() : "LIMA01")
            .addressStreet(guest.getAddress() != null ? guest.getAddress().getStreet() : "Dirección no proporcionada")
            .addressCity(guest.getAddress() != null ? guest.getAddress().getCity() : "Lima")
            .addressCountry(guest.getAddress() != null ? guest.getAddress().getCountry() : "Perú")
            .shippingCost(calculateShippingCostFromTotal(BigDecimal.valueOf(event.getTotalAmount())))
            .estimatedDelivery("3-5 días hábiles")
            .status("SHIPPING_CREATED")
            .shippedAt(LocalDateTime.now())
            .build();

    shipmentRepository.save(shipment);


        OrderShippedEvent shippedEvent = OrderShippedEvent.builder()
        .orderId(event.getOrderId())
        .trackingNumber(shipment.getTrackingNumber())
        .carrier("Serpost")
        .estimatedDelivery("3-5 días hábiles")
        .shippedAt(LocalDateTime.now())
        .message("Tu pedido ya está en camino")
        .build();

        // Publicar evento para que order-service actualice estado y notificaciones envíen email
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EVENTS_EXCHANGE,
                "order.shipped",
                shippedEvent
        );

        log.info("Envío creado con tracking {} para orden {}", shipment.getTrackingNumber(), event.getOrderId());
    }

    private BigDecimal calculateShippingCostFromTotal(BigDecimal total) {
        if (total == null) return new BigDecimal("15.00");
        if (total.compareTo(new BigDecimal("300")) >= 0) return BigDecimal.ZERO;
        if (total.compareTo(new BigDecimal("100")) >= 0) return new BigDecimal("10.00");
        return new BigDecimal("15.00");
    }
}