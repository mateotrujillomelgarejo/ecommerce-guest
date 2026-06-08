package pe.takiq.ecommerce.review_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.takiq.ecommerce.review_service.entity.Review;
import pe.takiq.ecommerce.review_service.entity.ReviewStatus;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByProductIdAndStatus(
            String productId, ReviewStatus status, Pageable pageable);

    Page<Review> findByStatus(ReviewStatus status, Pageable pageable);

    boolean existsByProductIdAndUserId(String productId, String userId);

    Optional<Review> findByProductIdAndUserId(String productId, String userId);

    long countByProductIdAndStatus(String productId, ReviewStatus status);

    @Query("SELECT AVG(r.rating) FROM Review r " +
           "WHERE r.productId = :productId AND r.status = 'APPROVED'")
    Double calculateAverageRating(@Param("productId") String productId);

    Page<Review> findByProductId(String productId, Pageable pageable);
}