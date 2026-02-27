package pe.takiq.ecommerce.cart_service.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import pe.takiq.ecommerce.cart_service.dto.ProductDTO;
import pe.takiq.ecommerce.cart_service.dto.request.PriceCalculationRequest;
import pe.takiq.ecommerce.cart_service.dto.response.PriceCalculationResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartIntegrationManager {

    private final InventoryClient inventoryClient;
    private final ProductClient productClient;
    private final PricingClient pricingClient;

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "checkStockFallback")
    public Boolean checkStock(String productId, Integer quantity) {
        return inventoryClient.checkStock(productId, quantity);
    }

    public Boolean checkStockFallback(String productId, Integer quantity, Throwable t) {
        log.warn("Fallback: Inventory-Service inalcanzable. Se asume sin stock para evitar overselling. Producto: {}", productId);
        return false;
    }

    @Retry(name = "productService")
    public ProductDTO getProduct(String productId) {
        return productClient.getProduct(productId);
    }

    @CircuitBreaker(name = "pricingService")
    public PriceCalculationResponse calculatePricing(PriceCalculationRequest request) {
        return pricingClient.calculate(request);
    }
}