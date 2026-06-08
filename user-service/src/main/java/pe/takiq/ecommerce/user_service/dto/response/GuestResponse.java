package pe.takiq.ecommerce.user_service.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Respuesta usada por Order Service y Shipping Service
 * para obtener datos del guest (incluida la dirección).
 * Mantiene el mismo contrato que tenía Customer Service.
 */
@Data
public class GuestResponse {
    private UUID id;
    private String sessionId;
    private String name;
    private String email;
    private String phone;
    private AddressResponse address;   // dirección de envío (la primera SHIPPING o BOTH)
    private LocalDateTime createdAt;
}
