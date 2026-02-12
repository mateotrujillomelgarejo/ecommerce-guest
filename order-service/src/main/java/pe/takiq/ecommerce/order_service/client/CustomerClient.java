package pe.takiq.ecommerce.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import pe.takiq.ecommerce.order_service.dto.GuestResponseDTO;


@FeignClient(
    name = "customer-service",
    url = "${customer-service.url:http://localhost:8085}"
)
public interface CustomerClient {

    @GetMapping("/guests/session/{sessionId}")
    GuestResponseDTO getGuestBySessionId(@PathVariable("sessionId") String sessionId);
}