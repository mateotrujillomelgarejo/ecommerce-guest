package pe.takiq.ecommerce.product_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import pe.takiq.ecommerce.product_service.model.Product;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByNameContainingIgnoreCase(String name);

    Page<Product> findByCategory(String category, Pageable pageable);

    Page<Product> findByPriceBetween(Double minPrice, Double maxPrice, Pageable pageable);

    Page<Product> findAllByOrderByAverageRatingDesc(Pageable pageable);

    Page<Product> findByTagsContaining(String tag, Pageable pageable);
}