package pe.takiq.ecommerce.order_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.takiq.ecommerce.order_service.model.Order;
import pe.takiq.ecommerce.order_service.model.OrderStatus;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, String> {

    @Query("SELECT COUNT(o) > 0 FROM Order o " +
           "JOIN o.items i " +
           "WHERE o.guestId = :userId " +
           "AND i.productId = :productId " +
           "AND o.status IN :statuses")
    boolean existsByUserIdAndProductIdAndStatusIn(
        @Param("userId") String userId,
        @Param("productId") String productId,
        @Param("statuses") List<OrderStatus> statuses
    );
    List<Order> findByStatus(OrderStatus status);
}