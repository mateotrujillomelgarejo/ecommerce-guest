package pe.takiq.ecommerce.pricing_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import pe.takiq.ecommerce.pricing_service.dto.ProductPriceDTO;

@FeignClient(name = "product-service", url = "${product-service.url:http://localhost:8081}")
public interface ProductClient {

    @GetMapping("/products/{id}")
    ProductPriceDTO getProductPrice(@PathVariable("id") String id);
}