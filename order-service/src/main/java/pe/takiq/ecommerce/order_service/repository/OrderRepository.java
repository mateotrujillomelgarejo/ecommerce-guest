package pe.takiq.ecommerce.order_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.takiq.ecommerce.order_service.model.Order;

public interface OrderRepository extends JpaRepository<Order, String> {

    boolean existsByPaymentId(String paymentId);

    Optional<Order> findByPaymentId(String paymentId);

    // Para consulta guest
    Optional<Order> findByIdAndEmail(String id, String email);

    // Opcional: para dashboard futuro
    List<Order> findByGuestIdOrderByCreatedAtDesc(String guestId);

}