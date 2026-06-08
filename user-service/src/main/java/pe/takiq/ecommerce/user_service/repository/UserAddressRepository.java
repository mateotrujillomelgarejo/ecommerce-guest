package pe.takiq.ecommerce.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.takiq.ecommerce.user_service.entity.UserAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {
    List<UserAddress> findAllByUserId(UUID userId);
    Optional<UserAddress> findByIdAndUserId(UUID id, UUID userId);
    long countByUserId(UUID userId);
    void deleteByIdAndUserId(UUID id, UUID userId);
}
