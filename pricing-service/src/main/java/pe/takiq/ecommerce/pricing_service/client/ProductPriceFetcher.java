package pe.takiq.ecommerce.pricing_service.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pe.takiq.ecommerce.pricing_service.dto.ProductPriceDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductPriceFetcher {

    private final ProductClient productClient;

    @CircuitBreaker(name = "productClient", fallbackMethod = "fetchPricesFallback")
    @Retry(name = "productClient")
    public List<ProductPriceDTO> fetchPricesConResiliencia(List<String> productIds) {
        return productClient.getBulkPrices(productIds);
    }

    public List<ProductPriceDTO> fetchPricesFallback(List<String> productIds, Throwable ex) {
        log.error("Product-Service inalcanzable para obtener precios base. Usando 0.0 temporalmente. Causa: {}", ex.getMessage());
        return productIds.stream()
                .map(id -> new ProductPriceDTO(id, BigDecimal.ZERO))
                .collect(Collectors.toList());
    }
}