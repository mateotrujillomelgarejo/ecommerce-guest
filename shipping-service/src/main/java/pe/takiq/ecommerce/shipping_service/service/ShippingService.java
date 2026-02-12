package pe.takiq.ecommerce.shipping_service.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.takiq.ecommerce.shipping_service.client.CustomerClient;
import pe.takiq.ecommerce.shipping_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.shipping_service.dto.GuestResponseDTO;
import pe.takiq.ecommerce.shipping_service.dto.ShippingCalculationRequest;
import pe.takiq.ecommerce.shipping_service.dto.ShippingCalculationResponse;
import pe.takiq.ecommerce.shipping_service.events.OrderCreatedEvent;
import pe.takiq.ecommerce.shipping_service.events.OrderShippedEvent;
import pe.takiq.ecommerce.shipping_service.model.Shipment;
import pe.takiq.ecommerce.shipping_service.repository.ShipmentRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingService {

    private final ShipmentRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final CustomerClient customerClient;

    public boolean existsShipment(String orderId) {
        return repository.findByOrderId(orderId).isPresent();
    }

    // ────────────────────────────────────────────────
    // ASYNC – post pago
    // ────────────────────────────────────────────────
    @Transactional
    public void createAndShip(OrderCreatedEvent event) {

        GuestResponseDTO guest = customerClient.getGuestBySessionId(event.getSessionId());

        BigDecimal shippingCost = calculateCost(
                event.getTotal()
        );

        Shipment shipment = Shipment.builder()
                .orderId(event.getOrderId())
                .postalCode(getPostal(guest))
                .addressStreet(getStreet(guest))
                .addressCity(getCity(guest))
                .addressCountry(getCountry(guest))
                .shippingCost(shippingCost)
                .estimatedDelivery("3-5 días hábiles")
                .status("SHIPPED")
                .shippedAt(LocalDateTime.now())
                .build();

        shipment = repository.save(shipment);

        publishShippedEvent(shipment, guest.getEmail());

        log.info("Shipment creado y SHIPPED → orderId={}, tracking={}",
                shipment.getOrderId(),
                shipment.getTrackingNumber());
    }

    private void publishShippedEvent(Shipment s, String email) {

        OrderShippedEvent event = OrderShippedEvent.builder()
                .orderId(s.getOrderId())
                .trackingNumber(s.getTrackingNumber())
                .carrier("Serpost")
                .estimatedDelivery(s.getEstimatedDelivery())
                .shippedAt(s.getShippedAt())
                .guestEmail(email)
                .message("Tu pedido ya está en camino")
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EVENTS_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY_SHIPPED,
                event
        );
    }

    // ────────────────────────────────────────────────
    // Quote síncrono
    // ────────────────────────────────────────────────
    public ShippingCalculationResponse calculateShipping(ShippingCalculationRequest req) {
        BigDecimal total = req.getOrderTotal() != null ? req.getOrderTotal() : BigDecimal.ZERO;

        BigDecimal cost = calculateCost(total);
        String msg = cost.equals(BigDecimal.ZERO)
                ? "¡Envío gratis por superar S/300!"
                : "Envío estándar";

        return new ShippingCalculationResponse(cost, "3-5 días hábiles", msg);
    }

    private BigDecimal calculateCost(BigDecimal total) {
        if (total.compareTo(new BigDecimal("300")) >= 0) return BigDecimal.ZERO;
        if (total.compareTo(new BigDecimal("100")) >= 0) return new BigDecimal("10.00");
        return new BigDecimal("15.00");
    }

    // Helpers guest
    private String getPostal(GuestResponseDTO g) {
        return g.getAddress() != null ? g.getAddress().getPostalCode() : "LIMA01";
    }
    private String getStreet(GuestResponseDTO g) {
        return g.getAddress() != null ? g.getAddress().getStreet() : "Dirección no registrada";
    }
    private String getCity(GuestResponseDTO g) {
        return g.getAddress() != null ? g.getAddress().getCity() : "Lima";
    }
    private String getCountry(GuestResponseDTO g) {
        return g.getAddress() != null ? g.getAddress().getCountry() : "Perú";
    }
}
