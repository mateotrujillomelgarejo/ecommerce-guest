package pe.takiq.ecommerce.order_service.Listener;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pe.takiq.ecommerce.order_service.config.RabbitMQConfig;
import pe.takiq.ecommerce.order_service.event.OrderDeliveredEvent;
import pe.takiq.ecommerce.order_service.event.OrderShippedEvent;
import pe.takiq.ecommerce.order_service.model.OrderStatus;
import pe.takiq.ecommerce.order_service.service.OrderService;

@Component
@RequiredArgsConstructor
public class ShippingEventListener {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_SHIPPED)
    public void handleOrderShipped(OrderShippedEvent event) {
        orderService.updateStatus(event.getOrderId(), OrderStatus.SHIPPED);
        // Opcional: guardar trackingNumber en order
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_DELIVERED)
    public void handleOrderDelivered(OrderDeliveredEvent event) {
        orderService.updateStatus(event.getOrderId(), OrderStatus.DELIVERED);
    }
}