package pe.takiq.ecommerce.search_service.service;

import pe.takiq.ecommerce.search_service.dto.SearchRequest;
import pe.takiq.ecommerce.search_service.dto.SearchResponse;
import pe.takiq.ecommerce.search_service.dto.SuggestResponse;
import pe.takiq.ecommerce.search_service.event.ProductUpdatedEvent;

public interface SearchService {
    SearchResponse search(SearchRequest request);
    SuggestResponse suggest(String query);
    void indexProduct(ProductUpdatedEvent event);
    void removeProduct(String productId);
}