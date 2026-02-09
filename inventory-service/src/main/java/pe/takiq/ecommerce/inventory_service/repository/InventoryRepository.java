package pe.takiq.ecommerce.inventory_service.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.takiq.ecommerce.inventory_service.model.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(String productId);

    @Query("SELECT i FROM Inventory i WHERE i.productId IN :ids AND i.active = true")
    List<Inventory> findActiveByProductIds(@Param("ids") Collection<String> ids);

    @Modifying
    @Query(value = """
        UPDATE inventory
        SET available_quantity = available_quantity - :qty,
            last_updated = CURRENT_TIMESTAMP
        WHERE product_id = :productId
          AND active = true
          AND available_quantity >= :qty
        """, nativeQuery = true)
    int deductStock(@Param("productId") String productId,
                    @Param("qty") int quantity);
}
