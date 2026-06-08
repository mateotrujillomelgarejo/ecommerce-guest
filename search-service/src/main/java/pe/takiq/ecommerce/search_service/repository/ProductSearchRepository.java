package pe.takiq.ecommerce.search_service.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import pe.takiq.ecommerce.search_service.document.ProductDocument;

public interface ProductSearchRepository
        extends ElasticsearchRepository<ProductDocument, String> {
}