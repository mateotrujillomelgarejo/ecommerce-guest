package pe.takiq.ecommerce.search_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pe.takiq.ecommerce.search_service.event.ProductUpdatedEvent;
import pe.takiq.ecommerce.search_service.service.SearchService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductUpdatedListener {

    private final SearchService searchService;

    @RabbitListener(queues = "${rabbitmq.queue.product-updated}")
    public void onProductUpdated(ProductUpdatedEvent event) {
        log.info("Evento product.updated recibido: productId={}, active={}",
                event.getProductId(), event.isActive());
        try {
            searchService.indexProduct(event);
        } catch (Exception e) {
            log.error("Error indexando productId={}: {}", event.getProductId(), e.getMessage(), e);
            throw e;
        }
    }
}