package pe.takiq.ecommerce.review_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RatingStatsResponse {
    private String productId;
    private double averageRating;
    private long reviewCount;
    private boolean cached;
}