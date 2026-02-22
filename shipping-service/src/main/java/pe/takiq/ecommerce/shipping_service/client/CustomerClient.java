package pe.takiq.ecommerce.shipping_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import pe.takiq.ecommerce.shipping_service.dto.GuestResponseDTO;
import pe.takiq.ecommerce.shipping_service.dto.AddressRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FeignClient(
    name = "customer-service",
    url = "${customer-service.url:http://localhost:8085}"
)
public interface CustomerClient {

    Logger log = LoggerFactory.getLogger(CustomerClient.class);

    @CircuitBreaker(name = "customerClient", fallbackMethod = "guestFallback")
    @Retry(name = "customerClient", fallbackMethod = "guestFallback")
    @GetMapping("/guests/session/{sessionId}")
    GuestResponseDTO getGuestBySessionId(@PathVariable("sessionId") String sessionId);

    default GuestResponseDTO guestFallback(String sessionId, Throwable t) {
        log.warn("Customer-Service inalcanzable para sessionId {}. Usando datos de envío por defecto. Causa: {}", sessionId, t.getMessage());
        
        GuestResponseDTO fallback = new GuestResponseDTO();
        fallback.setSessionId(sessionId);
        fallback.setGuestId("FALLBACK-GUEST");
        fallback.setName("Cliente Temporal");
        fallback.setEmail("soporte@tutienda.pe");
        fallback.setPhone("000000000");

        AddressRequestDTO address = new AddressRequestDTO();
        address.setStreet("Dirección pendiente de confirmación");
        address.setCity("Lima");
        address.setPostalCode("LIMA01");
        address.setCountry("Perú");
        fallback.setAddress(address);

        return fallback;
    }
}