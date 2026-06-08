package pe.takiq.ecommerce.review_service.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pe.takiq.ecommerce.review_service.dto.request.CreateReviewRequest;
import pe.takiq.ecommerce.review_service.dto.response.RatingStatsResponse;
import pe.takiq.ecommerce.review_service.dto.response.ReviewResponse;

import java.util.UUID;

public interface ReviewService {

    ReviewResponse createReview(String userId, CreateReviewRequest request);

    ReviewResponse markHelpful(UUID reviewId, String userId);

    Page<ReviewResponse> getApprovedReviewsByProduct(String productId, Pageable pageable);

    RatingStatsResponse getRatingStats(String productId);

    ReviewResponse approveReview(UUID reviewId);

    ReviewResponse rejectReview(UUID reviewId, String reason);

    Page<ReviewResponse> getPendingReviews(Pageable pageable);

    Page<ReviewResponse> getAllReviewsByProduct(String productId, Pageable pageable);
}