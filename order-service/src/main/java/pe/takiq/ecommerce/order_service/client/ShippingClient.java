package pe.takiq.ecommerce.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import pe.takiq.ecommerce.order_service.dto.request.ShippingCalculationRequest;
import pe.takiq.ecommerce.order_service.dto.response.ShippingCalculationResponse;

@FeignClient(name = "shipping-service", url = "${shipping-service.url:http://shipping:8091}")
public interface ShippingClient {
    @PostMapping("/shipping/calculate")
    ShippingCalculationResponse calculate(@RequestBody ShippingCalculationRequest request);
}