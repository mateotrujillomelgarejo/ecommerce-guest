package pe.takiq.ecommerce.search_service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import pe.takiq.ecommerce.search_service.event.StockRestockedEvent;


@Slf4j
@Component
@RequiredArgsConstructor
public class StockRestockedListener {

    @RabbitListener(queues = "${rabbitmq.queue.stock-restocked:search.stock-restocked.queue}")
    public void onStockRestocked(StockRestockedEvent event) {
        log.info("stock.restocked recibido: productId={}, quantityAdded={}, newStock={}",
                event.getProductId(), event.getQuantityAdded(), event.getNewAvailableQuantity());
    }
}