package pe.takiq.ecommerce.customer_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.takiq.ecommerce.customer_service.dto.AddressRequestDTO;
import pe.takiq.ecommerce.customer_service.dto.GuestRequestDTO;
import pe.takiq.ecommerce.customer_service.dto.GuestResponseDTO;
import pe.takiq.ecommerce.customer_service.model.Guest;
import pe.takiq.ecommerce.customer_service.repository.GuestRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GuestService {

    private final GuestRepository repository;
    private final GuestCacheService cacheService;

    @Value("${guest.cache.enabled:true}")
    private boolean cacheEnabled;

    @Transactional
    public GuestResponseDTO createGuest(GuestRequestDTO request) {
        if (cacheEnabled) {
            Optional<Guest> cached = cacheService.getGuestBySessionId(request.getSessionId());
            if (cached.isPresent()) return toResponse(cached.get());
        }

        Optional<Guest> existingBySession = repository.findBySessionId(request.getSessionId());
        if (existingBySession.isPresent()) {
            Guest guest = existingBySession.get();
            guest.setName(request.getName());
            guest.setEmail(request.getEmail());
            guest.setPhone(request.getPhone());
            guest = repository.save(guest);
            if (cacheEnabled) cacheService.saveGuest(guest);
            return toResponse(guest);
        }

        if (request.getEmail() != null) {
            Optional<Guest> existingByEmail = repository.findByEmail(request.getEmail());
            if (existingByEmail.isPresent()) {
                Guest returningGuest = existingByEmail.get();

                if (cacheEnabled) cacheService.deleteGuest(returningGuest);
                
                returningGuest.setSessionId(request.getSessionId());
                returningGuest.setName(request.getName());
                returningGuest.setPhone(request.getPhone());
                
                returningGuest = repository.save(returningGuest);
                if (cacheEnabled) cacheService.saveGuest(returningGuest);
                return toResponse(returningGuest);
            }
        }

        Guest guest = new Guest();
        guest.setSessionId(request.getSessionId());
        guest.setName(request.getName());
        guest.setEmail(request.getEmail());
        guest.setPhone(request.getPhone());

        Guest saved = repository.save(guest);

        if (cacheEnabled) {
            cacheService.saveGuest(saved);
        }

        return toResponse(saved);
    }

    public GuestResponseDTO getGuestBySessionId(String sessionId) {
        if (cacheEnabled) {
            Optional<Guest> cached = cacheService.getGuestBySessionId(sessionId);
            if (cached.isPresent()) return toResponse(cached.get());
        }

        Guest guest = repository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Guest no encontrado por sessionId: " + sessionId));
        
        if (cacheEnabled) cacheService.saveGuest(guest);
        return toResponse(guest);
    }

    public GuestResponseDTO getGuestByEmail(String email) {
        if (cacheEnabled) {
            Optional<Guest> cached = cacheService.getGuestByEmail(email);
            if (cached.isPresent()) return toResponse(cached.get());
        }

        Guest guest = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Guest no encontrado por email: " + email));
                
        if (cacheEnabled) cacheService.saveGuest(guest);
        return toResponse(guest);
    }

    @Transactional
    public GuestResponseDTO addAddress(String guestId, AddressRequestDTO request) {
        Guest guest = getGuestEntity(guestId);
        Guest.Address address = new Guest.Address();
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());
        guest.setAddress(address);

        Guest updated = repository.save(guest);
        if (cacheEnabled) {
            cacheService.saveGuest(updated);
        }
        return toResponse(updated);
    }

    private Guest getGuestEntity(String guestId) {
        if (cacheEnabled) {
            return cacheService.getGuestById(guestId)
                    .orElseGet(() -> repository.findById(guestId)
                            .orElseThrow(() -> new RuntimeException("Guest no encontrado")));
        } else {
            return repository.findById(guestId)
                    .orElseThrow(() -> new RuntimeException("Guest no encontrado"));
        }
    }

    private GuestResponseDTO toResponse(Guest guest) {
        GuestResponseDTO dto = new GuestResponseDTO();
        dto.setGuestId(guest.getId());
        dto.setSessionId(guest.getSessionId());
        dto.setName(guest.getName());
        dto.setEmail(guest.getEmail());
        dto.setPhone(guest.getPhone());
        if (guest.getAddress() != null) {
            AddressRequestDTO addrDto = new AddressRequestDTO();
            addrDto.setStreet(guest.getAddress().getStreet());
            addrDto.setCity(guest.getAddress().getCity());
            addrDto.setPostalCode(guest.getAddress().getPostalCode());
            addrDto.setCountry(guest.getAddress().getCountry());
            dto.setAddress(addrDto);
        }
        dto.setCreatedAt(guest.getCreatedAt());
        return dto;
    }
}