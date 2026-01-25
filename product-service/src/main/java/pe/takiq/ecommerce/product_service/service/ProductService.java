package pe.takiq.ecommerce.product_service.service;

import org.springframework.stereotype.Service;

import pe.takiq.ecommerce.product_service.model.Product;
import pe.takiq.ecommerce.product_service.repository.ProductRepository;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public List<Product> findAll() {
        return repository.findAll();
    }

    public Product findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    public Product save(Product product) {
        return repository.save(product);
    }
}