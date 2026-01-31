package pe.takiq.ecommerce.customer_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.takiq.ecommerce.customer_service.dto.AddressRequestDTO;
import pe.takiq.ecommerce.customer_service.dto.GuestRequestDTO;
import pe.takiq.ecommerce.customer_service.dto.GuestResponseDTO;
import pe.takiq.ecommerce.customer_service.service.GuestService;

@RestController
@RequestMapping("/guests")
@RequiredArgsConstructor
public class GuestController {

    private final GuestService service;

    @PostMapping
    public ResponseEntity<GuestResponseDTO> createGuest(@Valid @RequestBody GuestRequestDTO request) {
        return ResponseEntity.ok(service.createGuest(request));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<GuestResponseDTO> getBySessionId(@PathVariable String sessionId) {
        return ResponseEntity.ok(service.getGuestBySessionId(sessionId));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<GuestResponseDTO> getByEmail(@PathVariable String email) {
        return ResponseEntity.ok(service.getGuestByEmail(email));
    }

    @PostMapping("/{guestId}/address")
    public ResponseEntity<GuestResponseDTO> addAddress(
            @PathVariable String guestId,
            @Valid @RequestBody AddressRequestDTO request) {
        return ResponseEntity.ok(service.addAddress(guestId, request));
    }

    // Mantener otros endpoints si necesitas CRUD general
}