package pe.takiq.ecommerce.pricing_service.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import pe.takiq.ecommerce.pricing_service.dto.ProductPriceDTO;

@FeignClient(name = "product-service", url = "${product-service.url:http://localhost:8081}")
public interface ProductClient {

    @PostMapping("/products/bulk-prices")
    List<ProductPriceDTO> getBulkPrices(@RequestBody List<String> productIds);
}