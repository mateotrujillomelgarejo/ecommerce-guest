package pe.takiq.ecommerce.cart_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import pe.takiq.ecommerce.cart_service.dto.ProductDTO;

@FeignClient(
    name = "product-service",
    url = "${product-service.url}"
)
public interface ProductClient {

    @GetMapping("/products/{id}")
    ProductDTO getProductById(@PathVariable("id") Long id);

}
