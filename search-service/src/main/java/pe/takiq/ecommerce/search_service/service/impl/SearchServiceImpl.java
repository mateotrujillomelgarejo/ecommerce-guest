package pe.takiq.ecommerce.search_service.service.impl;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import pe.takiq.ecommerce.search_service.document.ProductDocument;
import pe.takiq.ecommerce.search_service.dto.*;
import pe.takiq.ecommerce.search_service.event.ProductUpdatedEvent;
import pe.takiq.ecommerce.search_service.repository.ProductSearchRepository;
import pe.takiq.ecommerce.search_service.service.SearchService;
import co.elastic.clients.elasticsearch.ElasticsearchClient;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchClient esClient;
    private final ProductSearchRepository searchRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${search.index.name}")
    private String indexName;

    @Value("${search.cache.ttl-seconds}")
    private long cacheTtlSeconds;

    @Value("${search.cache.suggest-ttl-seconds}")
    private long suggestTtlSeconds;

    private static final String CACHE_PREFIX_SEARCH  = "search:result:";
    private static final String CACHE_PREFIX_SUGGEST = "search:suggest:";

    // ─────────────────────────────────────────────────────────────────────────
    // BÚSQUEDA PRINCIPAL
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public pe.takiq.ecommerce.search_service.dto.SearchResponse search(
            pe.takiq.ecommerce.search_service.dto.SearchRequest request) {

        String cacheKey = buildSearchCacheKey(request);

        // 1. Verificar caché Redis — deserialización segura con try/catch
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                pe.takiq.ecommerce.search_service.dto.SearchResponse cachedResponse =
                        objectMapper.convertValue(cached,
                                pe.takiq.ecommerce.search_service.dto.SearchResponse.class);
                cachedResponse.setCached(true);
                log.debug("Cache hit para búsqueda: {}", request.getQ());
                return cachedResponse;
            }
        } catch (Exception e) {
            log.warn("Error leyendo caché de búsqueda para key {}: {}", cacheKey, e.getMessage());
        }

        // 2. Consultar Elasticsearch
        try {
            pe.takiq.ecommerce.search_service.dto.SearchResponse response =
                    executeElasticsearchQuery(request);

            // 3. Guardar en Redis
            redisTemplate.opsForValue().set(cacheKey, response, cacheTtlSeconds, TimeUnit.SECONDS);
            return response;

        } catch (IOException e) {
            log.error("Error ejecutando búsqueda en Elasticsearch: {}", e.getMessage(), e);
            return pe.takiq.ecommerce.search_service.dto.SearchResponse.builder()
                    .results(List.of())
                    .totalElements(0)
                    .totalPages(0)
                    .currentPage(request.getPage())
                    .pageSize(request.getSize())
                    .query(request.getQ())
                    .cached(false)
                    .build();
        }
    }

    private pe.takiq.ecommerce.search_service.dto.SearchResponse executeElasticsearchQuery(
            pe.takiq.ecommerce.search_service.dto.SearchRequest request) throws IOException {

        List<Query> mustQueries   = new ArrayList<>();
        List<Query> filterQueries = new ArrayList<>();

        // Solo productos activos — siempre
        filterQueries.add(Query.of(q -> q.term(t -> t.field("active").value(true))));

        // Full-text en nombre, descripción, tags y categoría
        if (request.getQ() != null && !request.getQ().isBlank()) {
            mustQueries.add(Query.of(q -> q.multiMatch(m -> m
                    .query(request.getQ())
                    .fields(List.of("name^3", "description^1", "tags^2", "category^1"))
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO")
            )));
        }

        // Filtro por categoría
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            filterQueries.add(Query.of(q -> q
                    .term(t -> t.field("category").value(request.getCategory()))));
        }

        // Filtro por subcategoría
        if (request.getSubcategory() != null && !request.getSubcategory().isBlank()) {
            filterQueries.add(Query.of(q -> q
                    .term(t -> t.field("subcategory").value(request.getSubcategory()))));
        }

        // Filtro por rango de precio
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            filterQueries.add(Query.of(q -> q.range(r -> {
                var range = r.field("price");
                if (request.getMinPrice() != null)
                    range = range.gte(co.elastic.clients.json.JsonData.of(request.getMinPrice()));
                if (request.getMaxPrice() != null)
                    range = range.lte(co.elastic.clients.json.JsonData.of(request.getMaxPrice()));
                return range;
            })));
        }

        // Filtro por rating mínimo
        if (request.getMinRating() != null) {
            filterQueries.add(Query.of(q -> q.range(r -> r
                    .field("averageRating")
                    .gte(co.elastic.clients.json.JsonData.of(request.getMinRating())))));
        }

        // Bool query final
        Query finalQuery = mustQueries.isEmpty()
                ? Query.of(q -> q.bool(b -> b.filter(filterQueries)))
                : Query.of(q -> q.bool(b -> b.must(mustQueries).filter(filterQueries)));

        int from = request.getPage() * request.getSize();

        SearchRequest esRequest = SearchRequest.of(s -> {
            var req = s.index(indexName)
                    .query(finalQuery)
                    .from(from)
                    .size(request.getSize());

            return switch (request.getSort()) {
                case PRICE_ASC   -> req.sort(so -> so.field(f -> f.field("price").order(SortOrder.Asc)));
                case PRICE_DESC  -> req.sort(so -> so.field(f -> f.field("price").order(SortOrder.Desc)));
                case RATING_DESC -> req.sort(so -> so.field(f -> f.field("averageRating").order(SortOrder.Desc)));
                case NEWEST      -> req.sort(so -> so.field(f -> f.field("updatedAt").order(SortOrder.Desc)));
                default          -> req; // RELEVANCE
            };
        });

        SearchResponse<ProductDocument> esResponse = esClient.search(esRequest, ProductDocument.class);

        long totalHits = esResponse.hits().total() != null
                ? esResponse.hits().total().value() : 0;

        List<SearchResultItem> items = esResponse.hits().hits().stream()
                .map(Hit::source)
                .filter(Objects::nonNull)
                .map(this::toResultItem)
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) totalHits / request.getSize());

        return pe.takiq.ecommerce.search_service.dto.SearchResponse.builder()
                .results(items)
                .totalElements(totalHits)
                .totalPages(totalPages)
                .currentPage(request.getPage())
                .pageSize(request.getSize())
                .query(request.getQ())
                .cached(false)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUTOCOMPLETE / SUGGEST
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public SuggestResponse suggest(String query) {
        if (query == null || query.isBlank()) {
            return SuggestResponse.builder()
                    .suggestions(List.of()).query(query).cached(false).build();
        }

        String cacheKey = CACHE_PREFIX_SUGGEST + query.toLowerCase().trim();

        // 1. Verificar caché — deserialización segura
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                SuggestResponse cachedResponse =
                        objectMapper.convertValue(cached, SuggestResponse.class);
                cachedResponse.setCached(true);
                return cachedResponse;
            }
        } catch (Exception e) {
            log.warn("Error leyendo caché de suggest para key {}: {}", cacheKey, e.getMessage());
        }

        // 2. Búsqueda prefix con search_as_you_type
        try {
            SearchRequest esRequest = SearchRequest.of(s -> s
                    .index(indexName)
                    .query(q -> q.bool(b -> b
                            .must(m -> m.multiMatch(mm -> mm
                                    .query(query)
                                    .fields(List.of(
                                            "name.suggest",
                                            "name.suggest._2gram",
                                            "name.suggest._3gram"
                                    ))
                                    .type(TextQueryType.BoolPrefix)
                            ))
                            .filter(f -> f.term(t -> t.field("active").value(true)))
                    ))
                    .size(8)
                    .source(src -> src.filter(f -> f.includes(List.of("name"))))
            );

            SearchResponse<ProductDocument> esResponse = esClient.search(esRequest, ProductDocument.class);

            List<String> suggestions = esResponse.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .map(ProductDocument::getName)
                    .distinct()
                    .limit(8)
                    .collect(Collectors.toList());

            SuggestResponse response = SuggestResponse.builder()
                    .suggestions(suggestions)
                    .query(query)
                    .cached(false)
                    .build();

            redisTemplate.opsForValue().set(cacheKey, response, suggestTtlSeconds, TimeUnit.SECONDS);
            return response;

        } catch (IOException e) {
            log.error("Error en autocomplete para query '{}': {}", query, e.getMessage());
            return SuggestResponse.builder()
                    .suggestions(List.of()).query(query).cached(false).build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INDEXACIÓN
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void indexProduct(ProductUpdatedEvent event) {
        if (!event.isActive()) {
            removeProduct(event.getProductId());
            return;
        }

        ProductDocument doc = ProductDocument.builder()
                .productId(event.getProductId())
                .name(event.getName())
                .description(event.getDescription())
                .category(event.getCategory())
                .subcategory(event.getSubcategory())
                .tags(event.getTags())
                .price(event.getPrice())
                .averageRating(event.getAverageRating())
                .reviewCount(event.getReviewCount())
                .imageUrl(event.getImageUrl())
                .active(true)
                .updatedAt(Instant.now())
                .build();

        searchRepository.save(doc);

        // ✅ Invalidar caché de búsqueda con SCAN (no-bloqueante)
        invalidateSearchCache(event.getCategory());

        log.info("Producto indexado en Elasticsearch: productId={}", event.getProductId());
    }

    @Override
    public void removeProduct(String productId) {
        searchRepository.deleteById(productId);
        // Al eliminar, invalidar toda la caché de búsqueda
        invalidateSearchCache(null);
        log.info("Producto eliminado del índice Elasticsearch: productId={}", productId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    private SearchResultItem toResultItem(ProductDocument doc) {
        return SearchResultItem.builder()
                .productId(doc.getProductId())
                .name(doc.getName())
                .category(doc.getCategory())
                .subcategory(doc.getSubcategory())
                .tags(doc.getTags())
                .price(doc.getPrice())
                .averageRating(doc.getAverageRating())
                .reviewCount(doc.getReviewCount())
                .imageUrl(doc.getImageUrl())
                .build();
    }

    private String buildSearchCacheKey(pe.takiq.ecommerce.search_service.dto.SearchRequest req) {
        return CACHE_PREFIX_SEARCH +
                Objects.toString(req.getQ(), "")           + ":" +
                Objects.toString(req.getCategory(), "")    + ":" +
                Objects.toString(req.getSubcategory(), "") + ":" +
                Objects.toString(req.getMinPrice(), "")    + ":" +
                Objects.toString(req.getMaxPrice(), "")    + ":" +
                Objects.toString(req.getMinRating(), "")   + ":" +
                req.getSort().name()                       + ":" +
                req.getPage()                              + ":" +
                req.getSize();
    }

    /**
     * Invalida claves de caché de búsqueda usando SCAN (no-bloqueante).
     * KEYS está prohibido en producción porque congela Redis completo.
     *
     * Si se pasa categoría, intenta invalidar solo las claves que la contengan.
     * Si categoría es null, invalida toda la caché de búsqueda.
     */
    private void invalidateSearchCache(String category) {
        ScanOptions options = ScanOptions.scanOptions()
                .match(CACHE_PREFIX_SEARCH + "*")
                .count(100)
                .build();

        try (var cursor = redisTemplate.scan(options)) {
            List<String> keysToDelete = new ArrayList<>();
            cursor.forEachRemaining(key -> {
                // Si no hay categoría o la clave contiene la categoría, borrar
                if (category == null || key.contains(":" + category + ":")) {
                    keysToDelete.add(key);
                }
            });
            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
                log.debug("Invalidadas {} claves de caché de búsqueda", keysToDelete.size());
            }
        } catch (Exception e) {
            log.warn("Error invalidando caché de búsqueda: {}", e.getMessage());
        }
    }
}