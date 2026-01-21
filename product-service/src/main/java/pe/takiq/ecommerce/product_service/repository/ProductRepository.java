package pe.takiq.ecommerce.product_service.repository;

import pe.takiq.ecommerce.product_service.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}