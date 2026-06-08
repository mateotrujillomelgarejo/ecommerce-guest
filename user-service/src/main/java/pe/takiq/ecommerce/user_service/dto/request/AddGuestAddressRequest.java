package pe.takiq.ecommerce.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Igual al contrato que tenía Customer Service.
 * Shipping Service llama a este endpoint para añadir la dirección
 * que necesita para crear el envío.
 */
@Data
public class AddGuestAddressRequest {
    @NotBlank(message = "Calle es requerida")
    private String street;
    private String city;
    private String postalCode;
    private String country;
    private Boolean isDefault = true;
}
