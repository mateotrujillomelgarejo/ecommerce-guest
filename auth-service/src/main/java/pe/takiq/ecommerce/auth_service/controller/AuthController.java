package pe.takiq.ecommerce.auth_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.takiq.ecommerce.auth_service.dto.AuthResponse;
import pe.takiq.ecommerce.auth_service.dto.LoginRequest;
import pe.takiq.ecommerce.auth_service.dto.RefreshRequest;
import pe.takiq.ecommerce.auth_service.dto.RegisterRequest;
import pe.takiq.ecommerce.auth_service.dto.TokenValidationResponse;
import pe.takiq.ecommerce.auth_service.service.AuthService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * POST /auth/login
     * Valida credenciales y devuelve access + refresh token.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * POST /auth/refresh
     * Rota el refresh token y devuelve nuevos tokens.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /**
     * POST /auth/logout
     * Revoca el access token y elimina el refresh token de Redis.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /auth/validate?token=...
     * Endpoint interno que usa el API Gateway (o los demás servicios) para validar un JWT
     * sin necesidad de conocer el secret. Auth es el único que firma y valida.
     */
    @GetMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validate(@RequestParam String token) {
        return ResponseEntity.ok(authService.validate(token));
    }
}