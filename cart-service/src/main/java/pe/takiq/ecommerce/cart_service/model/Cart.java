package pe.takiq.ecommerce.cart_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Id
    private String id;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public static Cart create() {
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID().toString());
        return cart;
    }
}