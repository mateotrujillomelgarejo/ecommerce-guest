package pe.takiq.ecommerce.events;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GuestCreatedEvent {
    private String guestId;
    private String sessionId;
    private String email;
}