package pe.takiq.ecommerce.search_service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class SearchResultItem {
    private String productId;
    private String name;
    private String category;
    private String subcategory;
    private List<String> tags;
    private BigDecimal price;
    private Double averageRating;
    private Integer reviewCount;
    private String imageUrl;
}