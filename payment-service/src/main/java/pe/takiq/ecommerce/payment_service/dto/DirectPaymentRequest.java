package pe.takiq.ecommerce.payment_service.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class DirectPaymentRequest {
    private BigDecimal amount;
    private String currency;
    private String guestId;
    private String sessionId;
    private String paymentMethod;
    private String guestEmail;
}