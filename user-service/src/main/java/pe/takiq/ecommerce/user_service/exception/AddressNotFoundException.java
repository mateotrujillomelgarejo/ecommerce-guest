package pe.takiq.ecommerce.user_service.exception;

import java.util.UUID;

public class AddressNotFoundException extends RuntimeException {
    public AddressNotFoundException(UUID id) {
        super("Dirección no encontrada: " + id);
    }
}
