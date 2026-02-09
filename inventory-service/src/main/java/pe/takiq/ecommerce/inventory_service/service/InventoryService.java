package pe.takiq.ecommerce.inventory_service.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.var;
import lombok.extern.slf4j.Slf4j;
import pe.takiq.ecommerce.inventory_service.event.OrderCreatedEvent;
import pe.takiq.ecommerce.inventory_service.exception.InsufficientStockException;
import pe.takiq.ecommerce.inventory_service.exception.ProductNotFoundException;
import pe.takiq.ecommerce.inventory_service.model.Inventory;
import pe.takiq.ecommerce.inventory_service.repository.InventoryRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository repository;

    // ────────────────────────────────────────────────
    // Verificación de stock (SYNC - carrito)
    // ────────────────────────────────────────────────
    @Transactional
    public boolean checkAvailability(String productId, int quantity) {
        Inventory inv = getActiveInventory(productId);
        return inv.getAvailableQuantity() >= quantity;
    }

    @Transactional
    public boolean checkMultipleAvailability(Map<String, Integer> items) {
        if (items.isEmpty()) return true;

        List<Inventory> inventories =
                repository.findActiveByProductIds(items.keySet());

        Map<String, Inventory> invMap = inventories.stream()
                .collect(Collectors.toMap(Inventory::getProductId, i -> i));

        for (var entry : items.entrySet()) {
            Inventory inv = invMap.get(entry.getKey());
            if (inv == null || inv.getAvailableQuantity() < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    // ────────────────────────────────────────────────
    // Descuento definitivo (ASYNC - post pago)
    // ────────────────────────────────────────────────
    @Transactional
    public void deductStock(OrderCreatedEvent event) {

        for (OrderCreatedEvent.OrderItemEvent item : event.getItems()) {

            int updated = repository.deductStock(
                    item.getProductId(),
                    item.getQuantity()
            );

            if (updated == 0) {
                throw new InsufficientStockException(
                        "Stock insuficiente para producto " + item.getProductId()
                );
            }
        }

        log.info("Stock descontado correctamente para orderId={}", event.getOrderId());
    }

    // ────────────────────────────────────────────────
    // Admin
    // ────────────────────────────────────────────────
    @Transactional
    public void updateStock(String productId, int quantity) {
        Inventory inv = getInventory(productId);
        inv.setAvailableQuantity(Math.max(0, quantity));
        repository.save(inv);
    }

    // ────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────
    private Inventory getActiveInventory(String productId) {
        return repository.findByProductId(productId)
                .filter(Inventory::isActive)
                .orElseThrow(() ->
                        new ProductNotFoundException("Producto no encontrado o inactivo: " + productId));
    }

    private Inventory getInventory(String productId) {
        return repository.findByProductId(productId)
                .orElseThrow(() ->
                        new ProductNotFoundException("Producto no encontrado: " + productId));
    }
}
