package pe.takiq.ecommerce.search_service.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SearchRequest {
    private String q;
    private String category;
    private String subcategory;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double minRating;
    private SortOption sort = SortOption.RELEVANCE;
    private int page = 0;
    private int size = 20;

    public enum SortOption {
        RELEVANCE,
        PRICE_ASC,
        PRICE_DESC,
        RATING_DESC,
        NEWEST
    }
}