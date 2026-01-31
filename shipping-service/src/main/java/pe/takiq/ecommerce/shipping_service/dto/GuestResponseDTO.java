package pe.takiq.ecommerce.shipping_service.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GuestResponseDTO {
    private String guestId;
    private String sessionId;
    private String name;
    private String email;
    private String phone;
    private AddressRequestDTO address;
    private LocalDateTime createdAt;
}