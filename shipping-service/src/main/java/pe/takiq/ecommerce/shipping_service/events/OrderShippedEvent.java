package pe.takiq.ecommerce.shipping_service.events;

import java.io.Serializable;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderShippedEvent implements Serializable {

    private String orderId;         // ID de la orden que ya fue enviada

    private String guestEmail;

    private String trackingNumber;      // Número de seguimiento generado por shipping-service

    private String carrier;             // Opcional: nombre de la empresa de envíos (ej: "Serpost", "DHL")

    private String estimatedDelivery;   // Ej: "3-5 días hábiles" o fecha aproximada

    private LocalDateTime shippedAt;    // Fecha/hora en que se marcó como enviado

    private String message;             // Mensaje opcional para el cliente (ej: "Tu pedido ya está en camino")
}