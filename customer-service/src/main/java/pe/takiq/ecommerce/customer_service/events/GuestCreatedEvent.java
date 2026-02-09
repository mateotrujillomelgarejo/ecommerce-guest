package pe.takiq.ecommerce.customer_service.events;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GuestCreatedEvent {
    private String guestId;
    private String sessionId;
    private String email;
}