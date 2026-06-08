package pe.takiq.ecommerce.user_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.takiq.ecommerce.user_service.dto.request.*;
import pe.takiq.ecommerce.user_service.dto.response.*;
import pe.takiq.ecommerce.user_service.service.UserService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Perfil, direcciones y preferencias")
public class UserController {

    private final UserService userService;

    // ── Perfil CLIENT / ADMIN ─────────────────────────────────────────────────

    @GetMapping("/{userId}")
    @Operation(summary = "Obtener perfil por UUID interno")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(userId)));
    }

    @GetMapping("/auth/{authUserId}")
    @Operation(summary = "Obtener perfil por authUserId (el userId del JWT)")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByAuthId(@PathVariable String authUserId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserByAuthId(authUserId)));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Actualizar perfil")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Perfil actualizado",
            userService.updateUser(userId, request)));
    }

    // ── Direcciones CLIENT / ADMIN ────────────────────────────────────────────

    @GetMapping("/{userId}/addresses")
    @Operation(summary = "Listar direcciones del usuario")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAddresses(userId)));
    }

    @PostMapping("/{userId}/addresses")
    @Operation(summary = "Agregar dirección (máx. 5)")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @PathVariable UUID userId,
            @Valid @RequestBody CreateAddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Dirección agregada", userService.addAddress(userId, request)));
    }

    @PutMapping("/{userId}/addresses/{addressId}")
    @Operation(summary = "Actualizar dirección")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable UUID userId,
            @PathVariable UUID addressId,
            @RequestBody UpdateAddressRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Dirección actualizada",
            userService.updateAddress(userId, addressId, request)));
    }

    @DeleteMapping("/{userId}/addresses/{addressId}")
    @Operation(summary = "Eliminar dirección")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable UUID userId,
            @PathVariable UUID addressId) {
        userService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.ok("Dirección eliminada", null));
    }

    // ── Preferencias ──────────────────────────────────────────────────────────

    @GetMapping("/{userId}/preferences")
    @Operation(summary = "Obtener preferencias del usuario")
    public ResponseEntity<ApiResponse<PreferencesResponse>> getPreferences(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getPreferences(userId)));
    }

    @PutMapping("/{userId}/preferences")
    @Operation(summary = "Actualizar preferencias del usuario")
    public ResponseEntity<ApiResponse<PreferencesResponse>> updatePreferences(
            @PathVariable UUID userId,
            @RequestBody UpdatePreferencesRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Preferencias actualizadas",
            userService.updatePreferences(userId, request)));
    }

    // ── GUEST ─────────────────────────────────────────────────────────────────
    // Mantienen el mismo contrato que Customer Service tenía en /guests/**
    // para que Order Service y Shipping Service no necesiten cambios.

    @PostMapping("/guests")
    @Operation(summary = "Crear/actualizar guest (upsert inteligente)",
               description = "Absorbe la responsabilidad de POST /guests del antiguo Customer Service")
    public ResponseEntity<ApiResponse<GuestResponse>> createOrUpdateGuest(
            @Valid @RequestBody CreateGuestRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(userService.createOrUpdateGuest(request)));
    }

    @GetMapping("/session/{sessionId}")
    @Operation(summary = "Obtener guest por sessionId",
               description = "Usado por Order Service y Shipping Service. Equivale a GET /guests/session/{id}")
    public ResponseEntity<ApiResponse<GuestResponse>> getGuestBySession(@PathVariable String sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getGuestBySessionId(sessionId)));
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Obtener guest por email")
    public ResponseEntity<ApiResponse<GuestResponse>> getGuestByEmail(@PathVariable String email) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getGuestByEmail(email)));
    }

    @PostMapping("/session/{sessionId}/address")
    @Operation(summary = "Agregar dirección de envío a guest",
               description = "Equivale a POST /guests/{id}/address del antiguo Customer Service")
    public ResponseEntity<ApiResponse<GuestResponse>> addGuestAddress(
            @PathVariable String sessionId,
            @Valid @RequestBody AddGuestAddressRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Dirección añadida",
            userService.addGuestAddress(sessionId, request)));
    }
}
