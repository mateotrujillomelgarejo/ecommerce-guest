package pe.takiq.ecommerce.cart_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.takiq.ecommerce.cart_service.model.Cart;

public interface CartRepository extends JpaRepository<Cart, String> {
}