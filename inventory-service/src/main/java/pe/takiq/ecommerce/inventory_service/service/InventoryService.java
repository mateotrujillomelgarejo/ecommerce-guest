package pe.takiq.ecommerce.inventory_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.takiq.ecommerce.events.OrderConfirmedEvent;
import pe.takiq.ecommerce.events.OrderCreatedEvent;
import pe.takiq.ecommerce.inventory_service.exception.InsufficientStockException;
import pe.takiq.ecommerce.inventory_service.exception.ProductNotFoundException;
import pe.takiq.ecommerce.inventory_service.model.Inventory;
import pe.takiq.ecommerce.inventory_service.repository.InventoryRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository repository;

    // ────────────────────────────────────────────────
    //               Verificación de stock
    // ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public boolean checkAvailability(String productId, int requestedQuantity) {
        Inventory inv = getActiveInventoryOrThrow(productId);
        return getFreeStock(inv) >= requestedQuantity;
    }

    @Transactional(readOnly = true)
    public boolean checkMultipleAvailability(Map<String, Integer> items) {
        if (items.isEmpty()) {
            return true;
        }

        Set<String> productIds = items.keySet();
        List<Inventory> inventories = repository.findActiveByProductIds(productIds);

        Map<String, Inventory> invMap = inventories.stream()
                .collect(Collectors.toMap(Inventory::getProductId, inventory -> inventory));

        for (Map.Entry<String, Integer> entry : items.entrySet()) {
            Inventory inv = invMap.get(entry.getKey());
            if (inv == null || getFreeStock(inv) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    // ────────────────────────────────────────────────
    //               Reserva (orden creada)
    // ────────────────────────────────────────────────

    @Transactional
    public void reserveFromOrder(OrderCreatedEvent event) {
        log.info("Intentando reservar stock para orden: {}", event.getOrderId());

        for (OrderCreatedEvent.OrderItemEvent item : event.getItems()) {
            int updated = repository.tryReserve(item.getProductId(), item.getQuantity());
            if (updated == 0) {
                throw new InsufficientStockException(
                        "No se pudo reservar " + item.getQuantity() +
                        " unidades del producto " + item.getProductId() +
                        " (orden: " + event.getOrderId() + ")"
                );
            }
        }

        log.info("Reserva completada para orden: {}", event.getOrderId());
    }

    // ────────────────────────────────────────────────
    //          Commit (orden confirmada / pagada)
    // ────────────────────────────────────────────────

    @Transactional
    public void commitFromOrder(OrderConfirmedEvent event) {
        log.info("Confirmando stock para orden: {}", event.getOrderId());

        for (OrderConfirmedEvent.OrderItemEvent item : event.getItems()) {
            int updated = repository.tryCommitReservation(item.getProductId(), item.getQuantity());
            if (updated == 0) {
                throw new IllegalStateException(
                        "No se pudo confirmar la reserva del producto " + item.getProductId()
                );
            }
        }

        log.info("Stock confirmado para orden: {}", event.getOrderId());
    }

    // ────────────────────────────────────────────────
    //                    Admin
    // ────────────────────────────────────────────────

    @Transactional
    public void updateStock(String productId, int newQuantity) {
        Inventory inv = getInventoryOrThrow(productId);
        inv.setAvailableQuantity(Math.max(0, newQuantity));
        repository.save(inv);

        log.info("Stock actualizado manualmente: {} → {}", productId, newQuantity);
    }

    // ────────────────────────────────────────────────
    //                Helpers privados
    // ────────────────────────────────────────────────

    private Inventory getActiveInventoryOrThrow(String productId) {
        return repository.findByProductId(productId)
                .filter(Inventory::isActive)
                .orElseThrow(() ->
                        new ProductNotFoundException("Producto no encontrado o inactivo: " + productId)
                );
    }

    private Inventory getInventoryOrThrow(String productId) {
        return repository.findByProductId(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException("Producto no encontrado: " + productId)
                );
    }

    private int getFreeStock(Inventory inv) {
        Integer reserved = inv.getReservedQuantity();
        return inv.getAvailableQuantity() - (reserved != null ? reserved : 0);
    }
}
