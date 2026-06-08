package pe.takiq.ecommerce.cart_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MergeCartRequestDTO {

    @NotBlank
    private String guestSessionId;

    @NotBlank
    private String userSessionId;
}