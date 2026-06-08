package pe.takiq.ecommerce.user_service.exception;

public class AddressLimitExceededException extends RuntimeException {
    public AddressLimitExceededException() {
        super("Límite de 5 direcciones por usuario alcanzado");
    }
}
