package pe.takiq.ecommerce.product_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pe.takiq.ecommerce.product_service.events.ProductRatingUpdatedEvent;
import pe.takiq.ecommerce.product_service.repository.ProductRepository;
import org.springframework.cache.CacheManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductRatingListener {

    private final ProductRepository productRepository;
    private final CacheManager cacheManager;

    @RabbitListener(queues = "${rabbitmq.queue.product-rating-updated}")
    public void onProductRatingUpdated(ProductRatingUpdatedEvent event) {
        log.info("Evento product.rating.updated recibido: productId={}, avg={}, count={}",
                event.getProductId(), event.getAverageRating(), event.getReviewCount());

        productRepository.findById(event.getProductId()).ifPresentOrElse(
            product -> {
                product.setAverageRating(event.getAverageRating());
                product.setReviewCount((int) event.getReviewCount());
                productRepository.save(product);
                var cache = cacheManager.getCache("productById");
                if (cache != null) {
                    cache.put(event.getProductId(), product);
                }

                log.info("Rating actualizado: productId={}, avg={}, count={}",
                        event.getProductId(), event.getAverageRating(), event.getReviewCount());
            },
            () -> log.warn("Producto no encontrado para actualizar rating: productId={}",
                    event.getProductId())
        );
    }
}