package pe.takiq.ecommerce.customer_service.service;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.takiq.ecommerce.customer_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.customer_service.dto.AddressRequestDTO;
import pe.takiq.ecommerce.customer_service.dto.GuestRequestDTO;
import pe.takiq.ecommerce.customer_service.dto.GuestResponseDTO;
import pe.takiq.ecommerce.customer_service.events.GuestCreatedEvent;
import pe.takiq.ecommerce.customer_service.model.Guest;
import pe.takiq.ecommerce.customer_service.repository.GuestRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GuestService {

    private final GuestRepository repository;
    private final GuestCacheService cacheService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${guest.cache.enabled:true}")
    private boolean cacheEnabled;

    @Transactional
    @Retry(name = "guestService")
    public GuestResponseDTO createGuest(GuestRequestDTO request) {
        Optional<Guest> existing = repository.findBySessionId(request.getSessionId());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        Guest guest = new Guest();
        guest.setSessionId(request.getSessionId());
        guest.setName(request.getName());
        guest.setEmail(request.getEmail());
        guest.setPhone(request.getPhone());

        Guest saved = repository.save(guest);

        GuestCreatedEvent event = GuestCreatedEvent.builder()
                .guestId(saved.getId())
                .sessionId(saved.getSessionId())
                .email(saved.getEmail())
                .build();
        rabbitTemplate.convertAndSend(RabbitMQConfig.GUEST_EVENTS_EXCHANGE, "guest.created", event);

        if (cacheEnabled) {
            cacheService.saveGuest(saved);
        }

        return toResponse(saved);
    }

    public GuestResponseDTO getGuestBySessionId(String sessionId) {
        Guest guest = repository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Guest no encontrado por sessionId"));
        return toResponse(guest);
    }

    public GuestResponseDTO getGuestByEmail(String email) {
        Guest guest = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Guest no encontrado por email"));
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
            return cacheService.getGuest(guestId)
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