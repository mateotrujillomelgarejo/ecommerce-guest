package pe.takiq.ecommerce.inventory_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.takiq.ecommerce.events.OrderConfirmedEvent;
import pe.takiq.ecommerce.inventory_service.model.Inventory;
import pe.takiq.ecommerce.inventory_service.repository.InventoryRepository;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository repository;

    public Inventory save(Inventory inventory){
        return repository.save(inventory);
    }

    public boolean checkStock(String productId, int quantity) {
        return repository.findById(productId)
                .map(inv -> inv.getStock() >= quantity)
                .orElse(false);
    }

    @Transactional
    public void decrementStock(OrderConfirmedEvent event) {

        for (OrderConfirmedEvent.OrderItemEvent item : event.getItems()) {

            String productId = item.getProductId();

            Inventory inv = repository.findById(productId)
                    .orElseThrow(() ->
                            new RuntimeException("Inventario no encontrado para " + productId)
                    );

            int newStock = inv.getStock() - item.getQuantity();

            if (newStock < 0) {
                throw new IllegalStateException(
                        "Stock insuficiente para " + productId
                );
            }

            inv.setStock(newStock);
            repository.save(inv);
        }
    }


    // Opcional: Reserva temporal (para evitar overselling en checkout)
    @Transactional
    public void reserveStock(String productId, int quantity) {
        // Implementa lógica con TTL o campo reserved (no en este MVP)
    }
}