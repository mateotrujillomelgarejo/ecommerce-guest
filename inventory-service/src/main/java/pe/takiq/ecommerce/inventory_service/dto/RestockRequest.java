package pe.takiq.ecommerce.inventory_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RestockRequest {

    @NotBlank
    private String productId;

    @Min(value = 1, message = "La cantidad de restock debe ser al menos 1")
    private int quantity;

    private String reason;
}