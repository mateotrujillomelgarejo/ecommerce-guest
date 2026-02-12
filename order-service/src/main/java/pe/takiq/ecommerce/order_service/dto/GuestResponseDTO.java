package pe.takiq.ecommerce.order_service.dto;

import lombok.Data;

@Data
public class GuestResponseDTO {
    private String guestId;
    private String sessionId;
    private String name;
    private String email;
    private String phone;
}
