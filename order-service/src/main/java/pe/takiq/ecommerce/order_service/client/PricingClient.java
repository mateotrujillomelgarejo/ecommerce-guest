package pe.takiq.ecommerce.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import pe.takiq.ecommerce.order_service.dto.request.PriceCalculationRequest;
import pe.takiq.ecommerce.order_service.dto.response.PriceCalculationResponse;

@FeignClient(name = "pricing-service", url = "${pricing-service.url:http://pricing:8090}")
public interface PricingClient {
    @PostMapping("/pricing/calculate")
    PriceCalculationResponse calculate(@RequestBody PriceCalculationRequest request);
}