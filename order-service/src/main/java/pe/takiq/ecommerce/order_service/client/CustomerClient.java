package pe.takiq.ecommerce.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import pe.takiq.ecommerce.order_service.dto.CustomerDTO;

@FeignClient(name = "customer-service", url = "${customer-service.url:http://customer:8085}")
public interface CustomerClient {
    @PostMapping("/guests")
    CustomerDTO createGuest(@RequestBody CustomerDTO request);

    @GetMapping("/guests/{id}")
    CustomerDTO getGuest(@PathVariable("id") String id);
}