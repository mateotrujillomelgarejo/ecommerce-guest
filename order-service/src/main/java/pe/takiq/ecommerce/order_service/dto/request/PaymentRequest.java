package pe.takiq.ecommerce.order_service.dto.request;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PaymentRequest {
    private String orderId;
    private BigDecimal amount;
    private String guestEmail;
    private String paymentMethod; // "CARD", "YAPE", "TRANSFER", etc.
    private String returnUrl;     // URL de retorno después del pago
}