package pe.takiq.ecommerce.review_service.dto.response;

import lombok.Builder;
import lombok.Data;
import pe.takiq.ecommerce.review_service.entity.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ReviewResponse {
    private UUID reviewId;
    private String productId;
    private String userId;
    private int rating;
    private String title;
    private String body;
    private boolean verified;
    private ReviewStatus status;
    private int helpfulCount;
    private String rejectionReason;
    private Instant createdAt;
    private Instant updatedAt;
}