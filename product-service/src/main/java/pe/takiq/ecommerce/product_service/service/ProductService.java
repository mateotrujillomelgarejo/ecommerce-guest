package pe.takiq.ecommerce.product_service.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pe.takiq.ecommerce.product_service.client.InventoryClient;
import pe.takiq.ecommerce.product_service.dto.ProductDetailDTO;  // Nuevo DTO para detalles
import pe.takiq.ecommerce.product_service.model.Product;
import pe.takiq.ecommerce.product_service.repository.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository repository;
    private final InventoryClient inventoryClient;

    @Cacheable("products")
    public Page<Product> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Cacheable("productById")
    public Product findById(String id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    public Product save(Product product) {
        return repository.save(product);
    }

    // Nueva: Búsqueda por nombre
    public List<Product> searchByName(String name) {
        return repository.findByNameContainingIgnoreCase(name);
    }

    // Nueva: Filtrado por categoría
    public Page<Product> filterByCategory(String category, Pageable pageable) {
        return repository.findByCategory(category, pageable);
    }

    // Nueva: Filtrado por precio
    public Page<Product> filterByPrice(Double min, Double max, Pageable pageable) {
        return repository.findByPriceBetween(min, max, pageable);
    }

    // Nueva: Por popularidad
    public Page<Product> getPopular(Pageable pageable) {
        return repository.findAllByOrderByAverageRatingDesc(pageable);
    }

    @CircuitBreaker(name = "inventoryClient", fallbackMethod = "detailFallback")
    @Retry(name = "inventoryClient")
    public ProductDetailDTO getDetails(String id, Integer quantity) {
        Product product = findById(id);
        Boolean available = inventoryClient.checkStock(id, quantity == null ? 1 : quantity);

        ProductDetailDTO dto = new ProductDetailDTO();
        dto.setProduct(product);
        dto.setAvailable(available);
        dto.setStockMessage(available ? "Disponible" : "Agotado");
        return dto;
    }

    public ProductDetailDTO detailFallback(String id, Integer quantity, Throwable ex) {
        ProductDetailDTO dto = new ProductDetailDTO();
        dto.setProduct(findById(id));
        dto.setAvailable(false);
        dto.setStockMessage("Inventario no disponible temporalmente");
        return dto;
    }
}