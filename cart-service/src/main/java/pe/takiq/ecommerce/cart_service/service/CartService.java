package pe.takiq.ecommerce.cart_service.service;

import org.springframework.stereotype.Service;

import pe.takiq.ecommerce.cart_service.model.Cart;
import pe.takiq.ecommerce.cart_service.model.CartItem;
import pe.takiq.ecommerce.cart_service.repository.CartRepository;

@Service
public class CartService {

    private final CartRepository repository;

    public CartService(CartRepository repository) {
        this.repository = repository;
    }

    public Cart createCart() {
        Cart cart = Cart.create();
        return repository.save(cart);
    }

    public Cart getCart(String cartId) {
        return repository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado"));
    }

    public Cart addItem(String cartId, CartItem item) {
        Cart cart = getCart(cartId);
        cart.getItems().add(item);
        return repository.save(cart);
    }

    public Cart removeItem(String cartId, Long itemId) {
        Cart cart = getCart(cartId);
        cart.getItems().removeIf(i -> i.getId().equals(itemId));
        return repository.save(cart);
    }
}