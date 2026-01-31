package pe.takiq.ecommerce.shipping_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import pe.takiq.ecommerce.shipping_service.dto.GuestResponseDTO;

@FeignClient(name = "customer-service", url = "${customer-service.url:http://customer:8085}")
public interface CustomerClient {

    @GetMapping("/guests/{guestId}")
    GuestResponseDTO getGuest(@PathVariable("guestId") String guestId);
}