package pe.takiq.ecommerce.review_service.exception;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private Instant timestamp;
}