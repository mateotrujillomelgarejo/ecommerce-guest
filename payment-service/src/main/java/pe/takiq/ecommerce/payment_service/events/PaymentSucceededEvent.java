package pe.takiq.ecommerce.payment_service.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSucceededEvent {
    private String orderId;           // Clave principal para que Order Service lo procese
    private String paymentId;         // Idempotency key
    private Double amount;
    private String gateway;
    private LocalDateTime confirmedAt;
    private String guestEmail;
}