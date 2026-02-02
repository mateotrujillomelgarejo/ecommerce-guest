package pe.takiq.ecommerce.events;

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
    private String paymentId;
    private String orderId;
    private Double amount;
    private String gateway;
    private LocalDateTime confirmedAt;
}