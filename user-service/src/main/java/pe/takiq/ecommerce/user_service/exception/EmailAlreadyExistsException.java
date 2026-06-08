package pe.takiq.ecommerce.user_service.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("Email ya registrado: " + email);
    }
}
