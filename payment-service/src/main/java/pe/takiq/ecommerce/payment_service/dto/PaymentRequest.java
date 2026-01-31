package pe.takiq.ecommerce.payment_service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private String orderId;
    private BigDecimal amount;
    private String guestEmail;
    private String paymentMethod; // "CARD", "YAPE", "TRANSFER", etc.
    private String returnUrl;     // URL de retorno después del pago
}