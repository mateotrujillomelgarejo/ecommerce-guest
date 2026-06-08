package pe.takiq.ecommerce.review_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateReviewRequest {

    @NotBlank(message = "El productId es obligatorio")
    private String productId;

    @Min(value = 1, message = "El rating mínimo es 1")
    @Max(value = 5, message = "El rating máximo es 5")
    private int rating;

    @NotBlank(message = "El título es obligatorio")
    @Size(min = 5, max = 150, message = "El título debe tener entre 5 y 150 caracteres")
    private String title;

    @NotBlank(message = "El cuerpo de la reseña es obligatorio")
    @Size(min = 20, max = 2000, message = "La reseña debe tener entre 20 y 2000 caracteres")
    private String body;
}