package pe.takiq.ecommerce.search_service.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class SearchException extends RuntimeException {
    private final HttpStatus status;

    public SearchException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}