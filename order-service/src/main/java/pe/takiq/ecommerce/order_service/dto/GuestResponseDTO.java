package pe.takiq.ecommerce.order_service.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class GuestResponseDTO {
    private UUID id;
    private String sessionId;
    private String name;
    private String email;
    private String phone;
}