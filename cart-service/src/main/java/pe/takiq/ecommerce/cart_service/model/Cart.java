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
    
    // CAMPOS DE CACHÉ DE PRECIOS PARA EVITAR LLAMAR A PRICING CONSTANTEMENTE
    private Double subtotal = 0.0;
    private Double discount = 0.0;
    private Double tax = 0.0;
    private Double shippingEstimate = 0.0;
    private Double total = 0.0;
    private boolean needsRecalculation = false; // Flag crucial de optimización

    public static Cart create(String sessionId) {
        Cart cart = new Cart();
        cart.setId(sessionId);
        cart.setLastModified(LocalDateTime.now());
        cart.setNeedsRecalculation(false);
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
        this.needsRecalculation = true;
    }

    public void addItem(CartItem item) {
        this.items.add(item);
        this.lastModified = LocalDateTime.now();
        this.needsRecalculation = true;
    }

    public void removeItem(String productId) {
        this.items.removeIf(i -> i.getProductId().equals(productId));
        this.lastModified = LocalDateTime.now();
        this.needsRecalculation = true;
    }
}