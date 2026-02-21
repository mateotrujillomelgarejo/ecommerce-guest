package pe.takiq.ecommerce.order_service.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PaymentSucceededEvent {
    private String orderId;           // Clave principal para que Order Service lo procese
    private String paymentId;         // Idempotency key
    private Double amount;
    private String gateway;
    private LocalDateTime confirmedAt;
    private String guestEmail;
}