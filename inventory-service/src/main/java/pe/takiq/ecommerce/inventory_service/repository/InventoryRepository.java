package pe.takiq.ecommerce.inventory_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.takiq.ecommerce.inventory_service.model.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, String> {
}