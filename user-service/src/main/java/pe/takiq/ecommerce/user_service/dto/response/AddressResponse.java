package pe.takiq.ecommerce.user_service.dto.response;

import lombok.Data;
import pe.takiq.ecommerce.user_service.entity.AddressType;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AddressResponse {
    private UUID id;
    private AddressType type;
    private String street;
    private String streetNumber;
    private String apartment;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private Boolean isDefault;
    private LocalDateTime createdAt;
}
