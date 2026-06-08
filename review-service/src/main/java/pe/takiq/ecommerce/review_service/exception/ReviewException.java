package pe.takiq.ecommerce.review_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ReviewException extends RuntimeException {
    private final HttpStatus status;

    public ReviewException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}