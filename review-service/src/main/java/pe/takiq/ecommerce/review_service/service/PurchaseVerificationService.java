package pe.takiq.ecommerce.review_service.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.takiq.ecommerce.review_service.client.OrderClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseVerificationService {

    private final OrderClient orderClient;

    @CircuitBreaker(name = "order-service", fallbackMethod = "purchaseFallback")
    @Retry(name = "order-service")
    public boolean verifyPurchase(String userId, String productId) {
        return orderClient.verifyPurchase(userId, productId);
    }

    public boolean purchaseFallback(String userId, String productId, Exception ex) {
        log.warn("Order Service no disponible para verificar compra. " +
                 "userId={}, productId={}. Reseña creada como no verificada.", userId, productId);
        return false;
    }
}