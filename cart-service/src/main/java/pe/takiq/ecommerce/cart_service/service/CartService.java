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
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;


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

    @CircuitBreaker(
        name = "inventoryClient",
        fallbackMethod = "inventoryFallback"
    )
    @Retry(name = "inventoryClient")
    public Cart addItem(String cartId, AddItemRequestDTO request) {

        Cart cart = getCartEntity(cartId);

        Boolean hasStock = inventoryClient.checkStock(
            request.getProductId(),
            request.getQuantity()
        );

        if (!Boolean.TRUE.equals(hasStock)) {
            throw new RuntimeException("Stock insuficiente para producto " + request.getProductId());
        }

        ProductDTO product = productClient.getProductById(request.getProductId());

        CartItem item = new CartItem();
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setPrice(product.getPrice());
        item.setQuantity(request.getQuantity());

        cart.getItems().add(item);
        return repository.save(cart);
    }

    public Cart inventoryFallback(
        String cartId,
        AddItemRequestDTO request,
        Throwable ex
    ) {
        throw new IllegalStateException(
            "Inventory/Product no disponible temporalmente. Intenta más tarde.",
            ex
        );
    }

    public Cart removeItem(String cartId, String productId) {
        Cart cart = getCartEntity(cartId);
        cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        return repository.save(cart);
    }
}
