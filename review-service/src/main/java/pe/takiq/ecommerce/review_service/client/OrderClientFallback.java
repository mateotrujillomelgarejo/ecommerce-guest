package pe.takiq.ecommerce.review_service.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderClientFallback implements OrderClient {

    @Override
    public boolean verifyPurchase(String userId, String productId) {
        log.warn("Order Service no disponible para verificar compra. " +
                 "userId={}, productId={}. Reseña creada como no verificada.", userId, productId);
        return false;
    }
}