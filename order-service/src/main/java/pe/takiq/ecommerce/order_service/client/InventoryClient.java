package pe.takiq.ecommerce.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import pe.takiq.ecommerce.order_service.dto.ReserveStockRequest;

@FeignClient(name = "inventory-service", url = "${inventory-service.url:http://localhost:8084}")
public interface InventoryClient {
    @PostMapping("/inventory/reserve")
    void reserveStock(@RequestBody ReserveStockRequest request);
}