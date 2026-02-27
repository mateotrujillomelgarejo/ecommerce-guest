package pe.takiq.ecommerce.order_service.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.takiq.ecommerce.order_service.model.Order;
import pe.takiq.ecommerce.order_service.model.OrderStatus;
import pe.takiq.ecommerce.order_service.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AbandonedOrderScheduler {

    private final OrderRepository orderRepository;

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cancelAbandonedOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        
        // Asumiendo que OrderRepository puede usar custom queries, si no, puedes agregar este método en el repository:
        // List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime date);
        // Aquí lo haremos filtrando en memoria por simplicidad (aunque es mejor en query DB)
        
        List<Order> abandonedOrders = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.PAYMENT_PENDING)
                .filter(o -> o.getCreatedAt().isBefore(threshold))
                .toList();

        for (Order order : abandonedOrders) {
            order.setStatus(OrderStatus.FAILED);
            order.setFailureReason("Expiración de tiempo de pago (Timeout)");
            orderRepository.save(order);
            log.info("Orden abandonada {} cancelada por inactividad del cliente.", order.getId());
        }
    }
}