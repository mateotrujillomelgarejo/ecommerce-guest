package pe.takiq.ecommerce.review_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRatingUpdatedEvent {
    private String productId;
    private double averageRating;
    private long reviewCount;
    private Instant updatedAt;
}