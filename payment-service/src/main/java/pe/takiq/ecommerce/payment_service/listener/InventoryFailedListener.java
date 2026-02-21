package pe.takiq.ecommerce.payment_service.listener;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import pe.takiq.ecommerce.payment_service.service.PaymentService;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryFailedListener {

    private final PaymentService paymentService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "payment.inventory.failed.queue", durable = "true"),
            exchange = @Exchange(value = "ecommerce.events", type = "topic", ignoreDeclarationExceptions = "true"),
            key = "inventory.failed"
    ))
    public void onInventoryFailed(InventoryFailedEvent event) {
        log.info("Recibido evento de inventario fallido para la orden {}. Motivo: {}. Iniciando reembolso...", 
                 event.getOrderId(), event.getReason());
                 
        paymentService.processRefundForFailedOrder(event.getOrderId());
    }

    @Data
    public static class InventoryFailedEvent {
        private String orderId;
        private String reason;
    }
}