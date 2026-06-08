package pe.takiq.ecommerce.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.takiq.ecommerce.user_service.entity.User;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByAuthUserId(String authUserId);
    Optional<User> findBySessionId(String sessionId);
    Optional<User> findByEmail(String email);
    Optional<User> findByAuthUserIdAndIsActiveTrue(String authUserId);
    Optional<User> findByIdAndIsActiveTrue(UUID id);
    boolean existsByEmail(String email);
    boolean existsByAuthUserId(String authUserId);
}
