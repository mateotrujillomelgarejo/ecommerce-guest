package pe.takiq.ecommerce.user_service.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.takiq.ecommerce.user_service.dto.request.*;
import pe.takiq.ecommerce.user_service.dto.response.*;
import pe.takiq.ecommerce.user_service.entity.*;
import pe.takiq.ecommerce.user_service.exception.*;
import pe.takiq.ecommerce.user_service.repository.*;
import pe.takiq.ecommerce.user_service.service.GuestCacheService;
import pe.takiq.ecommerce.user_service.service.UserService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final int MAX_ADDRESSES = 5;

    private final UserRepository userRepository;
    private final UserAddressRepository addressRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final GuestCacheService guestCacheService;

    // ══════════════════════════════════════════════════════════════════════════
    // USUARIOS CLIENT / ADMIN
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public UserResponse getUserById(UUID userId) {
        return toUserResponse(findActiveUserById(userId));
    }

    @Override
    public UserResponse getUserByAuthId(String authUserId) {
        User user = userRepository.findByAuthUserIdAndIsActiveTrue(authUserId)
            .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con authUserId: " + authUserId));
        return toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = findActiveUserById(userId);
        if (request.getFirstName()           != null) user.setFirstName(request.getFirstName());
        if (request.getLastName()            != null) user.setLastName(request.getLastName());
        if (request.getPhone()               != null) user.setPhone(request.getPhone());
        if (request.getDateOfBirth()         != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender()              != null) user.setGender(request.getGender());
        if (request.getNewsletterSubscribed() != null) user.setNewsletterSubscribed(request.getNewsletterSubscribed());
        if (user.getStatus() == UserStatus.REGISTERED
            && user.getFirstName() != null && user.getLastName() != null) {
            user.setStatus(UserStatus.ACTIVE);
        }
        return toUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void addToTotalSpent(String authUserId, BigDecimal amount) {
        userRepository.findByAuthUserId(authUserId).ifPresent(user -> {
            user.setTotalSpent(user.getTotalSpent().add(amount));
            userRepository.save(user);
            log.info("totalSpent actualizado para userId={}", authUserId);
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    // USUARIOS GUEST  — absorbe toda la lógica de Customer Service
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Upsert inteligente (idéntico al GuestService original):
     * 1. Busca por sessionId en Redis → si existe, actualiza y devuelve.
     * 2. Busca por sessionId en DB    → si existe, actualiza y devuelve.
     * 3. Busca por email en DB        → usuario que regresó con nueva sesión.
     * 4. Crea uno nuevo.
     */
    @Override
    @Transactional
    public GuestResponse createOrUpdateGuest(CreateGuestRequest request) {

        // 1. Caché Redis por sessionId
        Optional<User> cached = guestCacheService.getBySessionId(request.getSessionId());
        if (cached.isPresent()) {
            User guest = cached.get();
            updateGuestFields(guest, request);
            guest = userRepository.save(guest);
            guestCacheService.saveGuest(guest);
            return toGuestResponse(guest);
        }

        // 2. DB por sessionId
        Optional<User> bySession = userRepository.findBySessionId(request.getSessionId());
        if (bySession.isPresent()) {
            User guest = bySession.get();
            updateGuestFields(guest, request);
            guest = userRepository.save(guest);
            guestCacheService.saveGuest(guest);
            return toGuestResponse(guest);
        }

        // 3. DB por email → guest que regresó con nueva sesión
        if (request.getEmail() != null) {
            Optional<User> byEmail = userRepository.findByEmail(request.getEmail());
            if (byEmail.isPresent() && byEmail.get().getStatus() == UserStatus.GUEST) {
                User returningGuest = byEmail.get();
                guestCacheService.evict(returningGuest);          // eliminar índice viejo
                returningGuest.setSessionId(request.getSessionId());
                updateGuestFields(returningGuest, request);
                returningGuest = userRepository.save(returningGuest);
                guestCacheService.saveGuest(returningGuest);
                return toGuestResponse(returningGuest);
            }
        }

        // 4. Crear nuevo guest
        User newGuest = User.builder()
            .sessionId(request.getSessionId())
            .email(request.getEmail())
            .firstName(extractFirstName(request.getName()))
            .lastName(extractLastName(request.getName()))
            .phone(request.getPhone())
            .status(UserStatus.GUEST)
            .isActive(true)
            .build();
        newGuest = userRepository.save(newGuest);
        guestCacheService.saveGuest(newGuest);
        log.info("Nuevo guest creado: sessionId={}", request.getSessionId());
        return toGuestResponse(newGuest);
    }

    @Override
    public GuestResponse getGuestBySessionId(String sessionId) {
        // Primero busca en caché
        Optional<User> cached = guestCacheService.getBySessionId(sessionId);
        if (cached.isPresent()) return toGuestResponse(cached.get());

        User guest = userRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new UserNotFoundException("Guest no encontrado: sessionId=" + sessionId));
        guestCacheService.saveGuest(guest);
        return toGuestResponse(guest);
    }

    @Override
    public GuestResponse getGuestByEmail(String email) {
        Optional<User> cached = guestCacheService.getByEmail(email);
        if (cached.isPresent()) return toGuestResponse(cached.get());

        User guest = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("Guest no encontrado: email=" + email));
        guestCacheService.saveGuest(guest);
        return toGuestResponse(guest);
    }

    @Override
    @Transactional
    public GuestResponse addGuestAddress(String sessionId, AddGuestAddressRequest request) {
        final User guest = userRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new UserNotFoundException("Guest no encontrado: sessionId=" + sessionId));

        final UUID guestId = guest.getId();

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.findAllByUserId(guestId).stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                .forEach(a -> {
                    a.setIsDefault(false);
                    addressRepository.save(a);
                });
        }

        UserAddress address = UserAddress.builder()
            .user(guest)
            .type(AddressType.SHIPPING)
            .street(request.getStreet())
            .city(request.getCity())
            .postalCode(request.getPostalCode())
            .country(request.getCountry() != null ? request.getCountry() : "PE")
            .isDefault(request.getIsDefault() != null ? request.getIsDefault() : true)
            .build();
        addressRepository.save(address);

        User refreshedGuest = userRepository.findById(guestId).orElseThrow();
        guestCacheService.saveGuest(refreshedGuest);
        log.info("Dirección añadida a guest sessionId={}", sessionId);
        return toGuestResponse(refreshedGuest);
    }


    // ══════════════════════════════════════════════════════════════════════════
    // DIRECCIONES (CLIENT / ADMIN)
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public List<AddressResponse> getAddresses(UUID userId) {
        findActiveUserById(userId);
        return addressRepository.findAllByUserId(userId).stream()
            .map(this::toAddressResponse)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressResponse addAddress(UUID userId, CreateAddressRequest request) {
        User user = findActiveUserById(userId);
        if (addressRepository.countByUserId(userId) >= MAX_ADDRESSES) {
            throw new AddressLimitExceededException();
        }
        if (Boolean.TRUE.equals(request.getIsDefault())) clearDefaultAddresses(userId);

        UserAddress address = UserAddress.builder()
            .user(user)
            .type(request.getType())
            .street(request.getStreet())
            .streetNumber(request.getStreetNumber())
            .apartment(request.getApartment())
            .city(request.getCity())
            .state(request.getState())
            .postalCode(request.getPostalCode())
            .country(request.getCountry())
            .isDefault(request.getIsDefault())
            .build();
        return toAddressResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(UUID userId, UUID addressId, UpdateAddressRequest request) {
        findActiveUserById(userId);
        UserAddress address = addressRepository.findByIdAndUserId(addressId, userId)
            .orElseThrow(() -> new AddressNotFoundException(addressId));

        if (request.getType()         != null) address.setType(request.getType());
        if (request.getStreet()       != null) address.setStreet(request.getStreet());
        if (request.getStreetNumber() != null) address.setStreetNumber(request.getStreetNumber());
        if (request.getApartment()    != null) address.setApartment(request.getApartment());
        if (request.getCity()         != null) address.setCity(request.getCity());
        if (request.getState()        != null) address.setState(request.getState());
        if (request.getPostalCode()   != null) address.setPostalCode(request.getPostalCode());
        if (request.getCountry()      != null) address.setCountry(request.getCountry());
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefaultAddresses(userId);
            address.setIsDefault(true);
        }
        return toAddressResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        findActiveUserById(userId);
        if (addressRepository.findByIdAndUserId(addressId, userId).isEmpty()) {
            throw new AddressNotFoundException(addressId);
        }
        addressRepository.deleteByIdAndUserId(addressId, userId);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PREFERENCIAS
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public PreferencesResponse getPreferences(UUID userId) {
        findActiveUserById(userId);
        return preferencesRepository.findByUserId(userId)
            .map(this::toPreferencesResponse)
            .orElse(new PreferencesResponse());
    }

    @Override
    @Transactional
    public PreferencesResponse updatePreferences(UUID userId, UpdatePreferencesRequest request) {
        User user = findActiveUserById(userId);
        UserPreferences prefs = preferencesRepository.findByUserId(userId)
            .orElse(UserPreferences.builder().user(user).build());

        if (request.getLanguage()          != null) prefs.setLanguage(request.getLanguage());
        if (request.getTimezone()          != null) prefs.setTimezone(request.getTimezone());
        if (request.getCurrency()          != null) prefs.setCurrency(request.getCurrency());
        if (request.getNotificationsEmail() != null) prefs.setNotificationsEmail(request.getNotificationsEmail());
        if (request.getNotificationsSms()  != null) prefs.setNotificationsSms(request.getNotificationsSms());
        if (request.getDarkMode()          != null) prefs.setDarkMode(request.getDarkMode());

        return toPreferencesResponse(preferencesRepository.save(prefs));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LISTENER DE EVENTOS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Llamado cuando llega el evento user.registered de auth-service.
     * Crea el perfil del usuario en estado INCOMPLETE.
     * Si ya existe (idempotencia), lo ignora.
     */
    @Override
    @Transactional
    public void createUserFromEvent(String authUserId, String email,
                                    String firstName, String lastName, String phone) {
    if (userRepository.existsByAuthUserId(authUserId)) {
        log.info("Usuario {} ya existe, ignorando evento duplicado", authUserId);
        return;
    }

    Optional<User> existingGuest = userRepository.findByEmail(email);
    if (existingGuest.isPresent() && existingGuest.get().getStatus() == UserStatus.GUEST) {
        User guest = existingGuest.get();
        guest.setAuthUserId(authUserId);
        guest.setStatus(UserStatus.REGISTERED);
        guest.setIsActive(true);
        // Crear preferencias si no las tiene
        if (guest.getPreferences() == null) {
            UserPreferences prefs = UserPreferences.builder().user(guest).build();
            guest.setPreferences(prefs);
        }
        guest.setSessionId(null);
        guestCacheService.evict(guest);
        userRepository.save(guest);
        log.info("Guest promovido a REGISTERED: authUserId={}, email={}", authUserId, email);
        return;
    }

        User user = User.builder()
            .authUserId(authUserId)
            .email(email)
            .firstName(firstName)
            .lastName(lastName)
            .phone(phone)
            .status(UserStatus.REGISTERED)
            .isActive(true)
            .build();

        UserPreferences prefs = UserPreferences.builder().user(user).build();
        user.setPreferences(prefs);
        userRepository.save(user);
        log.info("Perfil creado para usuario registrado: authUserId={}, email={}", authUserId, email);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS PRIVADOS
    // ══════════════════════════════════════════════════════════════════════════

    private User findActiveUserById(UUID userId) {
        return userRepository.findByIdAndIsActiveTrue(userId)
            .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: id=" + userId));
    }

    private void clearDefaultAddresses(UUID userId) {
        addressRepository.findAllByUserId(userId).stream()
            .filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
            .forEach(a -> { a.setIsDefault(false); addressRepository.save(a); });
    }

    private void updateGuestFields(User guest, CreateGuestRequest request) {
        if (request.getName()  != null) {
            guest.setFirstName(extractFirstName(request.getName()));
            guest.setLastName(extractLastName(request.getName()));
        }
        if (request.getEmail() != null) guest.setEmail(request.getEmail());
        if (request.getPhone() != null) guest.setPhone(request.getPhone());
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return null;
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts[0];
    }

    private String extractLastName(String fullName) {
        if (fullName == null || fullName.isBlank()) return null;
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts.length > 1 ? parts[1] : "";
    }

    // ── Mappers manuales (evitar dependencia de MapStruct para estos casos) ──

    private UserResponse toUserResponse(User user) {
        UserResponse r = new UserResponse();
        r.setId(user.getId());
        r.setAuthUserId(user.getAuthUserId());
        r.setSessionId(user.getSessionId());
        r.setEmail(user.getEmail());
        r.setFirstName(user.getFirstName());
        r.setLastName(user.getLastName());
        r.setPhone(user.getPhone());
        r.setDateOfBirth(user.getDateOfBirth());
        r.setGender(user.getGender());
        r.setStatus(user.getStatus());
        r.setNewsletterSubscribed(user.getNewsletterSubscribed());
        r.setTotalSpent(user.getTotalSpent());
        r.setIsActive(user.getIsActive());
        r.setRegistrationDate(user.getRegistrationDate());
        r.setUpdatedAt(user.getUpdatedAt());
        if (user.getAddresses() != null) {
            r.setAddresses(user.getAddresses().stream()
                .map(this::toAddressResponse).collect(Collectors.toList()));
        }
        return r;
    }

    GuestResponse toGuestResponse(User user) {
        GuestResponse r = new GuestResponse();
        r.setId(user.getId());
        r.setSessionId(user.getSessionId());
        r.setName(buildFullName(user));
        r.setEmail(user.getEmail());
        r.setPhone(user.getPhone());
        r.setCreatedAt(user.getRegistrationDate());

        // Dirección de envío principal (la que necesita Shipping Service)
        if (user.getAddresses() != null) {
            user.getAddresses().stream()
                .filter(a -> a.getType() == AddressType.SHIPPING || a.getType() == AddressType.BOTH)
                .findFirst()
                .or(() -> user.getAddresses().stream().findFirst())
                .ifPresent(a -> r.setAddress(toAddressResponse(a)));
        }
        return r;
    }

    private AddressResponse toAddressResponse(UserAddress a) {
        AddressResponse r = new AddressResponse();
        r.setId(a.getId());
        r.setType(a.getType());
        r.setStreet(a.getStreet());
        r.setStreetNumber(a.getStreetNumber());
        r.setApartment(a.getApartment());
        r.setCity(a.getCity());
        r.setState(a.getState());
        r.setPostalCode(a.getPostalCode());
        r.setCountry(a.getCountry());
        r.setIsDefault(a.getIsDefault());
        r.setCreatedAt(a.getCreatedAt());
        return r;
    }

    private PreferencesResponse toPreferencesResponse(UserPreferences p) {
        PreferencesResponse r = new PreferencesResponse();
        r.setId(p.getId());
        r.setLanguage(p.getLanguage());
        r.setTimezone(p.getTimezone());
        r.setCurrency(p.getCurrency());
        r.setNotificationsEmail(p.getNotificationsEmail());
        r.setNotificationsSms(p.getNotificationsSms());
        r.setDarkMode(p.getDarkMode());
        r.setCreatedAt(p.getCreatedAt());
        return r;
    }

    private String buildFullName(User user) {
        if (user.getFirstName() == null) return null;
        return (user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "")).trim();
    }
}
