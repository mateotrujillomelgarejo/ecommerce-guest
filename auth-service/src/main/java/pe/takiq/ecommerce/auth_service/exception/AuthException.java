package pe.takiq.ecommerce.auth_service.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {

    private final HttpStatus status;

    public AuthException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public static AuthException emailAlreadyExists() {
        return new AuthException("El email ya está registrado", HttpStatus.CONFLICT);
    }

    public static AuthException invalidCredentials() {
        return new AuthException("Credenciales inválidas", HttpStatus.UNAUTHORIZED);
    }

    public static AuthException userNotFound() {
        return new AuthException("Usuario no encontrado", HttpStatus.NOT_FOUND);
    }

    public static AuthException userDisabled() {
        return new AuthException("Cuenta deshabilitada", HttpStatus.FORBIDDEN);
    }

    public static AuthException invalidRefreshToken() {
        return new AuthException("Refresh token inválido o expirado", HttpStatus.UNAUTHORIZED);
    }

    public static AuthException refreshTokenMismatch() {
        return new AuthException("Refresh token no corresponde a la sesión activa", HttpStatus.UNAUTHORIZED);
    }
}