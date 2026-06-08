package pe.takiq.ecommerce.review_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", fallback = OrderClientFallback.class)
public interface OrderClient {

    @GetMapping("/orders/verify-purchase")
    boolean verifyPurchase(
            @RequestParam("userId") String userId,
            @RequestParam("productId") String productId
    );
}