package pe.takiq.ecommerce.product_service.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdatedEvent {
    private String productId;
    private String name;
    private String description;
    private String category;
    private String subcategory;
    private List<String> tags;
    private BigDecimal price;
    private Double averageRating;
    private Integer reviewCount;
    private String imageUrl;
    private boolean active;
}