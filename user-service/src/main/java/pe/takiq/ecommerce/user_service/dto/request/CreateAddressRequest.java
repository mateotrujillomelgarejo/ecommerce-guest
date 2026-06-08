package pe.takiq.ecommerce.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pe.takiq.ecommerce.user_service.entity.AddressType;

@Data
public class CreateAddressRequest {
    @NotNull
    private AddressType type;
    @NotBlank(message = "Calle es requerida")
    private String street;
    private String streetNumber;
    private String apartment;
    @NotBlank(message = "Ciudad es requerida")
    private String city;
    private String state;
    private String postalCode;
    @NotBlank(message = "País es requerido")
    private String country;
    private Boolean isDefault = false;
}
