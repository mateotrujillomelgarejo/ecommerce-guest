package pe.takiq.ecommerce.search_service.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.takiq.ecommerce.search_service.dto.*;
import pe.takiq.ecommerce.search_service.service.SearchService;

import java.math.BigDecimal;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subcategory,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "RELEVANCE") SearchRequest.SortOption sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        SearchRequest request = new SearchRequest();
        request.setQ(q);
        request.setCategory(category);
        request.setSubcategory(subcategory);
        request.setMinPrice(minPrice);
        request.setMaxPrice(maxPrice);
        request.setMinRating(minRating);
        request.setSort(sort);
        request.setPage(Math.max(0, page));
        request.setSize(Math.min(Math.max(1, size), 100));

        return ResponseEntity.ok(searchService.search(request));
    }

    @GetMapping("/suggest")
    public ResponseEntity<SuggestResponse> suggest(
            @RequestParam @NotBlank @Size(min = 2, max = 50) String q) {
        return ResponseEntity.ok(searchService.suggest(q));
    }

    @GetMapping("/categories")
    public ResponseEntity<SearchResponse> byCategory(
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        SearchRequest request = new SearchRequest();
        request.setCategory(category);
        request.setPage(page);
        request.setSize(size);
        request.setSort(SearchRequest.SortOption.RELEVANCE);

        return ResponseEntity.ok(searchService.search(request));
    }
}