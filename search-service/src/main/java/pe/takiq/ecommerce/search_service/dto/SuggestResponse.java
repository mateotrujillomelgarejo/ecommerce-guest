package pe.takiq.ecommerce.search_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SuggestResponse {
    private List<String> suggestions;
    private String query;
    private boolean cached;
}