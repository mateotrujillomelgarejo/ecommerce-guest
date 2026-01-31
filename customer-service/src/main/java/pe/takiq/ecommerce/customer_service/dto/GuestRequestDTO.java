package pe.takiq.ecommerce.customer_service.dto;

import lombok.Data;

@Data
public class GuestRequestDTO {
    private String sessionId;
    private String name;
    private String email;
    private String phone;
}