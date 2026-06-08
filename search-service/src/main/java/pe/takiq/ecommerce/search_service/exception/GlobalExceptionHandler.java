package pe.takiq.ecommerce.search_service.exception;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SearchException.class)
    public ResponseEntity<ErrorResponse> handleSearch(SearchException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(buildError(ex.getStatus().value(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Error inesperado en Search Service", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(500, "Error interno del servidor"));
    }

    private ErrorResponse buildError(int status, String message) {
        return ErrorResponse.builder()
                .status(status)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    @Data
    @Builder
    public static class ErrorResponse {
        private int status;
        private String message;
        private Instant timestamp;
    }
}