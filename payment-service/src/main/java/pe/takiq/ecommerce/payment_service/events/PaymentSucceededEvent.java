package pe.takiq.ecommerce.payment_service.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSucceededEvent {
    private String orderId;
    private String paymentId;
    private BigDecimal amount;
    private String gateway;
    private Instant confirmedAt;
    private String guestEmail;
}