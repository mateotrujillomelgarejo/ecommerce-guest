package pe.takiq.ecommerce.product_service.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import pe.takiq.ecommerce.product_service.client.InventoryClient;
import pe.takiq.ecommerce.product_service.dto.ProductDetailDTO;
import pe.takiq.ecommerce.product_service.dto.ProductPriceDTO;
import pe.takiq.ecommerce.product_service.events.ProductUpdatedEvent;
import pe.takiq.ecommerce.product_service.model.Product;
import pe.takiq.ecommerce.product_service.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository repository;
    private final InventoryClient inventoryClient;
    private final RabbitTemplate rabbitTemplate;

    @Autowired
    @Lazy
    private ProductService self;

    @Cacheable(value = "productsContent", key = "#pageable")
    public List<Product> findAllContent(Pageable pageable) {
        return repository.findAll(pageable).getContent();
    }

    @Cacheable(value = "productsTotal", key = "'total'")
    public long countAll() {
        return repository.count();
    }

    public Page<Product> findAll(Pageable pageable) {
        List<Product> content = findAllContent(pageable);
        long total = countAll();
        return new PageImpl<>(content, pageable, total);
    }

    @Cacheable(value = "productById", key = "#id")
    public Product findById(String id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    @Caching(
        evict = { 
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "productsByCategory", allEntries = true),
            @CacheEvict(value = "popularProducts", allEntries = true)
        },
        put = { @CachePut(value = "productById", key = "#result.id") }
    )
    public Product save(Product product) {
        Product saved = repository.save(product);
        
        ProductUpdatedEvent event = ProductUpdatedEvent.builder()
                .productId(saved.getId())
                .name(saved.getName())
                .price(BigDecimal.valueOf(saved.getPrice()))
                .active(saved.isActive())
                .build();
                
        rabbitTemplate.convertAndSend("ecommerce.events", "product.updated", event);
        
        return saved;
    }

    public List<Product> searchByName(String name) {
        return repository.findByNameContainingIgnoreCase(name);
    }

    @Cacheable(value = "productsByCategory", key = "#category + '-' + #pageable.pageNumber")
    public Page<Product> filterByCategory(String category, Pageable pageable) {
        return repository.findByCategory(category, pageable);
    }

    public Page<Product> filterByPrice(Double min, Double max, Pageable pageable) {
        return repository.findByPriceBetween(min, max, pageable);
    }

    @Cacheable(value = "popularProducts", key = "#pageable.pageNumber")
    public Page<Product> getPopular(Pageable pageable) {
        return repository.findAllByOrderByAverageRatingDesc(pageable);
    }

    @CircuitBreaker(name = "inventoryClient", fallbackMethod = "detailFallback")
    @Retry(name = "inventoryClient")
    public ProductDetailDTO getDetails(String id, Integer quantity) {
        Product product = self.findById(id); 
        Boolean available = inventoryClient.checkStock(id, quantity == null ? 1 : quantity);

        ProductDetailDTO dto = new ProductDetailDTO();
        dto.setProduct(product);
        dto.setAvailable(available);
        dto.setStockMessage(available ? "Disponible" : "Agotado");
        return dto;
    }

    public ProductDetailDTO detailFallback(String id, Integer quantity, Throwable ex) {
        ProductDetailDTO dto = new ProductDetailDTO();
        dto.setProduct(self.findById(id));
        dto.setAvailable(false);
        dto.setStockMessage("Inventario no disponible temporalmente");
        return dto;
    }

    public List<ProductPriceDTO> getBulkPrices(List<String> productIds) {
        Iterable<Product> products = repository.findAllById(productIds);
        
        List<ProductPriceDTO> results = new java.util.ArrayList<>();
        for (Product p : products) {
            BigDecimal price = p.getPrice() != null ? BigDecimal.valueOf(p.getPrice()) : BigDecimal.ZERO;
            results.add(new ProductPriceDTO(p.getId(), price));
        }
        return results;
    }
}