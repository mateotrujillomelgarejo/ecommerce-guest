package pe.takiq.ecommerce.search_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResponse {
    private List<SearchResultItem> results;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private String query;
    private boolean cached;
}