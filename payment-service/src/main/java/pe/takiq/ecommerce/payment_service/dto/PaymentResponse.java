package pe.takiq.ecommerce.payment_service.dto;

import lombok.Data;

@Data
public class PaymentResponse {
    private String paymentId;
    private String status;
    private String redirectUrl;   // Para 3DS o pasarela externa
    private String message;
}