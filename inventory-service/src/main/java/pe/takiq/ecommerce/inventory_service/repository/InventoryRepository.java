package pe.takiq.ecommerce.inventory_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.takiq.ecommerce.inventory_service.model.Inventory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(String productId);

    @Query("SELECT i FROM Inventory i WHERE i.productId IN :productIds AND i.active = true")
    List<Inventory> findActiveByProductIds(@Param("productIds") Collection<String> productIds);

    // Reserva: solo si hay stock libre suficiente
    @Modifying
    @Query(
    value =
        "UPDATE inventory " +
        "SET reserved_quantity = COALESCE(reserved_quantity, 0) + :quantity, " +
        "    last_updated = CURRENT_TIMESTAMP " +
        "WHERE product_id = :productId " +
        "  AND active = true " +
        "  AND (available_quantity - COALESCE(reserved_quantity, 0)) >= :quantity",
    nativeQuery = true
    )
    int tryReserve(@Param("productId") String productId, @Param("quantity") int quantity);


    // Commit: resta de available y libera reserva
    @Modifying
    @Query(
    value =
        "UPDATE inventory " +
        "SET available_quantity = available_quantity - :quantity, " +
        "    reserved_quantity = GREATEST(COALESCE(reserved_quantity, 0) - :quantity, 0), " +
        "    last_updated = CURRENT_TIMESTAMP " +
        "WHERE product_id = :productId " +
        "  AND active = true " +
        "  AND COALESCE(reserved_quantity, 0) >= :quantity",
    nativeQuery = true
    )
    int tryCommitReservation(@Param("productId") String productId, @Param("quantity") int quantity);


    // Decremento directo (para casos admin o correcciones)
    @Modifying
    @Query("UPDATE Inventory i SET i.availableQuantity = i.availableQuantity - :quantity, i.lastUpdated = CURRENT_TIMESTAMP WHERE i.productId = :productId AND i.availableQuantity >= :quantity")
    int decreaseStockDirect(@Param("productId") String productId, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE Inventory i SET i.availableQuantity = i.availableQuantity + :quantity, i.lastUpdated = CURRENT_TIMESTAMP WHERE i.productId = :productId")
    int increaseStock(@Param("productId") String productId, @Param("quantity") int quantity);
}