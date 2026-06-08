package pe.takiq.ecommerce.order_service.event;

import java.math.BigDecimal;
import java.time.Instant;

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
    private String orderId;
    private String paymentId;
    private BigDecimal amount;
    private String gateway;
    private Instant confirmedAt;
    private String guestEmail;
}