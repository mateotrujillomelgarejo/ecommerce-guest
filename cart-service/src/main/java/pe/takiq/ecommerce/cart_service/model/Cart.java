package pe.takiq.ecommerce.cart_service.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class Cart {
    private String id;
    private List<CartItem> items = new ArrayList<>();
    private LocalDateTime lastModified;

    public static Cart create(String sessionId) {
        Cart cart = new Cart();
        cart.setId(sessionId);
        cart.setLastModified(LocalDateTime.now());
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
        this.lastModified = LocalDateTime.now();
    }
}