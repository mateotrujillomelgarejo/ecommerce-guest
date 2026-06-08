package pe.takiq.ecommerce.auth_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.takiq.ecommerce.auth_service.domain.AuthUser;

import java.util.Optional;
import java.util.UUID;

public interface AuthUserRepository extends JpaRepository<AuthUser, UUID> {
    Optional<AuthUser> findByEmail(String email);
    boolean existsByEmail(String email);
}