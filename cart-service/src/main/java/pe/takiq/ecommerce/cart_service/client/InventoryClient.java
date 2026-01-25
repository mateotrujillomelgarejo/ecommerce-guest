package pe.takiq.ecommerce.cart_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service", url = "${inventory-service.url:http://localhost:8084}")
public interface InventoryClient {

    @GetMapping("/inventory/{productId}/check")
    Boolean checkStock(@PathVariable("productId") String productId, 
                       @RequestParam("quantity") Integer quantity);
}