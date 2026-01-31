package pe.takiq.ecommerce.cart_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Cart {
    @Id
    private String id;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    private LocalDateTime lastModified;

    @PrePersist @PreUpdate
    protected void updateTimestamp() {
        this.lastModified = LocalDateTime.now();
    }

    public static Cart create(String sessionId) {
        Cart cart = new Cart();
        cart.setId(sessionId);
        return cart;
    }

    public void updateQuantity(String productId, int newQuantity) {
        items.stream()
             .filter(i -> i.getProductId().equals(productId))
             .findFirst()
             .ifPresent(item -> {
                 if (newQuantity <= 0) {
                     items.remove(item);
                 } else {
                     item.setQuantity(newQuantity);
                 }
             });
    }
}