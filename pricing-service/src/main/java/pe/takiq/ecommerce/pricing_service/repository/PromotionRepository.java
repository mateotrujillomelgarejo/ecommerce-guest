package pe.takiq.ecommerce.pricing_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.takiq.ecommerce.pricing_service.model.Promotion;

import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    List<Promotion> findByProductIdAndActiveTrue(String productId);

    List<Promotion> findByCategoryAndActiveTrue(String category);
}