package pe.takiq.ecommerce.user_service.service;

import pe.takiq.ecommerce.user_service.dto.request.*;
import pe.takiq.ecommerce.user_service.dto.response.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface UserService {

    // ── Usuarios CLIENT/ADMIN ─────────────────────────────────────────────────
    UserResponse getUserById(UUID userId);
    UserResponse getUserByAuthId(String authUserId);
    UserResponse updateUser(UUID userId, UpdateUserRequest request);
    void addToTotalSpent(String authUserId, BigDecimal amount);

    // ── Usuarios GUEST ────────────────────────────────────────────────────────
    GuestResponse createOrUpdateGuest(CreateGuestRequest request);
    GuestResponse getGuestBySessionId(String sessionId);
    GuestResponse getGuestByEmail(String email);
    GuestResponse addGuestAddress(String sessionId, AddGuestAddressRequest request);

    // ── Direcciones (CLIENT/ADMIN) ────────────────────────────────────────────
    List<AddressResponse> getAddresses(UUID userId);
    AddressResponse addAddress(UUID userId, CreateAddressRequest request);
    AddressResponse updateAddress(UUID userId, UUID addressId, UpdateAddressRequest request);
    void deleteAddress(UUID userId, UUID addressId);

    // ── Preferencias ──────────────────────────────────────────────────────────
    PreferencesResponse getPreferences(UUID userId);
    PreferencesResponse updatePreferences(UUID userId, UpdatePreferencesRequest request);

    // ── Llamado desde el listener de user.registered ──────────────────────────
    void createUserFromEvent(String authUserId, String email,
                             String firstName, String lastName, String phone);
}
