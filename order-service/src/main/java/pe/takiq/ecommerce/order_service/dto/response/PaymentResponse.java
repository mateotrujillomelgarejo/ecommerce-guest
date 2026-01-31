package pe.takiq.ecommerce.order_service.dto.response;

import lombok.Data;

@Data
public class PaymentResponse {
    private String paymentId;
    private String status;
    private String redirectUrl;   // Para 3DS o pasarela externa
    private String message;
}