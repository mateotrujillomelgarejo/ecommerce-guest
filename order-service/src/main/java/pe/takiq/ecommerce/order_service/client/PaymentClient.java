package pe.takiq.ecommerce.order_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import pe.takiq.ecommerce.order_service.dto.request.PaymentRequest;
import pe.takiq.ecommerce.order_service.dto.response.PaymentResponse;

@FeignClient(name = "payment-service", url = "${payment-service.url:http://payment:8092}")
public interface PaymentClient {
    @PostMapping("/payments/initiate")
    PaymentResponse initiatePayment(@RequestBody PaymentRequest request);
}