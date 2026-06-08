package pe.takiq.ecommerce.user_service.dto.request;

import lombok.Data;
import pe.takiq.ecommerce.user_service.entity.AddressType;

@Data
public class UpdateAddressRequest {
    private AddressType type;
    private String street;
    private String streetNumber;
    private String apartment;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private Boolean isDefault;
}
