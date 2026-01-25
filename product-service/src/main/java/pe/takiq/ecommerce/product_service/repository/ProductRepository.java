package pe.takiq.ecommerce.product_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pe.takiq.ecommerce.product_service.model.Product;

public interface ProductRepository extends MongoRepository<Product, String> {
}