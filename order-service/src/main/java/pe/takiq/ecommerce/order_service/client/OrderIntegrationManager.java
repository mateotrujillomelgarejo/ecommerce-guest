package pe.takiq.ecommerce.order_service.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.takiq.ecommerce.order_service.dto.GuestResponseDTO;
import pe.takiq.ecommerce.order_service.dto.ReserveStockRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderIntegrationManager {

    private final CustomerClient customerClient;
    private final InventoryClient inventoryClient;

    @CircuitBreaker(name = "customerClient")
    @Retry(name = "customerClient")
    public GuestResponseDTO getGuest(String sessionId) {
        return customerClient.getGuestBySessionId(sessionId);
    }

    @CircuitBreaker(name = "inventoryClient")
    @Retry(name = "inventoryClient")
    public void reserveStock(ReserveStockRequest request) {
        inventoryClient.reserveStock(request);
    }
}