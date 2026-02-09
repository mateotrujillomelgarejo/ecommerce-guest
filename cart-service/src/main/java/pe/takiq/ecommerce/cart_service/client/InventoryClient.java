package pe.takiq.ecommerce.cart_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "inventory-service", url = "${inventory-service.url:http://localhost:8084}")
public interface InventoryClient {

@GetMapping("inventory/{productId}/check?quantity={quantity}")

Boolean checkStock(@PathVariable("productId") String productId, @PathVariable("quantity") Integer quantity);

}