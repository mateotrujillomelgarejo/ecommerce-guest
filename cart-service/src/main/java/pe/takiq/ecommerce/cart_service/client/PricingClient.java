package pe.takiq.ecommerce.cart_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import pe.takiq.ecommerce.cart_service.dto.request.PriceCalculationRequest;
import pe.takiq.ecommerce.cart_service.dto.response.PriceCalculationResponse;

@FeignClient(name = "pricing-service", url = "${pricing-service.url:http://localhost:8090}")
public interface PricingClient {

    @PostMapping("/pricing/calculate")
    PriceCalculationResponse calculatePrice(@RequestBody PriceCalculationRequest req);
}