package pe.takiq.ecommerce.cart_service.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import pe.takiq.ecommerce.cart_service.client.InventoryClient;
import pe.takiq.ecommerce.cart_service.client.ProductClient;
import pe.takiq.ecommerce.cart_service.dto.ProductDTO;
import pe.takiq.ecommerce.cart_service.dto.request.AddItemRequestDTO;
import pe.takiq.ecommerce.cart_service.model.Cart;
import pe.takiq.ecommerce.cart_service.model.CartItem;
import pe.takiq.ecommerce.cart_service.repository.CartRepository;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository repository;
    private final InventoryClient inventoryClient;
    private final ProductClient productClient;

    public Cart createCart() {
        return repository.save(Cart.create());
    }

    public Cart getCartEntity(String cartId) {
        return repository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Carrito no encontrado"));
    }

    public Cart addItem(String cartId, AddItemRequestDTO request) {
        Cart cart = getCartEntity(cartId);

        String productIdStr = request.getProductId().toString();

        boolean hasStock = inventoryClient.checkStock(
            request.getProductId().toString(),
            request.getQuantity()
        );

        if (!hasStock) {
            throw new RuntimeException("Stock insuficiente para producto " + request.getProductId());
        }

        ProductDTO product = productClient.getProductById(productIdStr);

        CartItem item = new CartItem();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setPrice(product.getPrice());
        item.setQuantity(request.getQuantity());

        cart.getItems().add(item);
        return repository.save(cart);
    }

    public Cart removeItem(String cartId, String productId) {
        Cart cart = getCartEntity(cartId);
        cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        return repository.save(cart);
    }
}
