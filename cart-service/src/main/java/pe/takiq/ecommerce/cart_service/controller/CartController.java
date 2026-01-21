package pe.takiq.ecommerce.cart_service.controller;

import org.springframework.web.bind.annotation.*;

import pe.takiq.ecommerce.cart_service.model.Cart;
import pe.takiq.ecommerce.cart_service.model.CartItem;
import pe.takiq.ecommerce.cart_service.service.CartService;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @PostMapping
    public Cart createCart() {
        return service.createCart();
    }

    @GetMapping("/{cartId}")
    public Cart getCart(@PathVariable String cartId) {
        return service.getCart(cartId);
    }

    @PostMapping("/{cartId}/items")
    public Cart addItem(
            @PathVariable String cartId,
            @RequestBody CartItem item) {
        return service.addItem(cartId, item);
    }

    @DeleteMapping("/{cartId}/items/{itemId}")
    public Cart removeItem(
            @PathVariable String cartId,
            @PathVariable Long itemId) {
        return service.removeItem(cartId, itemId);
    }
}