package pe.takiq.ecommerce.shipping_service.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Spring Transactional

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import pe.takiq.ecommerce.shipping_service.client.CustomerClient;
import pe.takiq.ecommerce.shipping_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.shipping_service.dto.AddressRequestDTO;
import pe.takiq.ecommerce.shipping_service.dto.GuestResponseDTO;
import pe.takiq.ecommerce.shipping_service.dto.ShippingCalculationRequest;
import pe.takiq.ecommerce.shipping_service.dto.ShippingCalculationResponse;
import pe.takiq.ecommerce.shipping_service.events.OrderPaidEvent;
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

    @CircuitBreaker(name = "customerClient", fallbackMethod = "guestFallback")
    public GuestResponseDTO getGuestForQuote(String sessionId) {
        return customerClient.getGuestBySessionId(sessionId);
    }

    public GuestResponseDTO guestFallback(String sessionId, Throwable t) {
        log.warn("Customer-Service inalcanzable para cotización. Usando fallback genérico.");
        GuestResponseDTO fallback = new GuestResponseDTO();
        fallback.setSessionId(sessionId);
        AddressRequestDTO address = new AddressRequestDTO();
        address.setPostalCode("LIMA01");
        fallback.setAddress(address);
        return fallback;
    }

    public ShippingCalculationResponse calculateShipping(ShippingCalculationRequest req) {
        BigDecimal total = req.getOrderTotal() != null ? req.getOrderTotal() : BigDecimal.ZERO;
        BigDecimal cost = calculateCost(total);
        String msg = cost.equals(BigDecimal.ZERO) ? "¡Envío gratis por superar S/300!" : "Envío estándar";
        return new ShippingCalculationResponse(cost, "3-5 días hábiles", msg);
    }


    @Retry(name = "customerClient")
    public GuestResponseDTO getGuestMandatory(String sessionId) {
        return customerClient.getGuestBySessionId(sessionId);
    }

    @Transactional
    public void createAndShip(OrderPaidEvent event) {

        GuestResponseDTO guest = getGuestMandatory(event.getSessionId());

        if (guest.getAddress() == null || guest.getAddress().getStreet() == null) {
            throw new IllegalStateException("El cliente no tiene una dirección válida registrada. No se puede enviar la orden: " + event.getOrderId());
        }

        BigDecimal shippingCost = calculateCost(event.getTotal());

        Shipment shipment = Shipment.builder()
                .orderId(event.getOrderId())
                .postalCode(guest.getAddress().getPostalCode())
                .addressStreet(guest.getAddress().getStreet())
                .addressCity(guest.getAddress().getCity())
                .addressCountry(guest.getAddress().getCountry())
                .shippingCost(shippingCost)
                .estimatedDelivery("3-5 días hábiles")
                .status("SHIPPED")
                .shippedAt(LocalDateTime.now())
                .build();

        shipment = repository.save(shipment);
        
        publishShippedEvent(shipment, guest.getEmail());

        log.info("Shipment creado y SHIPPED → orderId={}, tracking={}",
                shipment.getOrderId(), shipment.getTrackingNumber());
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

        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EVENTS_EXCHANGE, RabbitMQConfig.ROUTING_KEY_SHIPPED, event);
    }

    private BigDecimal calculateCost(BigDecimal total) {
        if (total.compareTo(new BigDecimal("300")) >= 0) return BigDecimal.ZERO;
        if (total.compareTo(new BigDecimal("100")) >= 0) return new BigDecimal("10.00");
        return new BigDecimal("15.00");
    }
}