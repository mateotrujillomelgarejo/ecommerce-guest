package pe.takiq.ecommerce.cart_service.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.experimental.var;
import pe.takiq.ecommerce.cart_service.client.InventoryClient;
import pe.takiq.ecommerce.cart_service.client.PricingClient;
import pe.takiq.ecommerce.cart_service.client.ProductClient;
import pe.takiq.ecommerce.cart_service.dto.ProductDTO;
import pe.takiq.ecommerce.cart_service.dto.request.AddItemRequestDTO;
import pe.takiq.ecommerce.cart_service.dto.request.PriceCalculationRequest;
import pe.takiq.ecommerce.cart_service.dto.request.UpdateItemRequestDTO;
import pe.takiq.ecommerce.cart_service.dto.response.CartResponseDTO;
import pe.takiq.ecommerce.cart_service.dto.response.PriceCalculationResponse;
import pe.takiq.ecommerce.cart_service.mapper.CartMapper;
import pe.takiq.ecommerce.cart_service.model.Cart;
import pe.takiq.ecommerce.cart_service.model.CartItem;
import pe.takiq.ecommerce.cart_service.repository.CartRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;


@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository repository;        // opcional ahora
    private final CartCacheService cacheService;
    private final InventoryClient inventoryClient;
    private final ProductClient productClient;
    private final PricingClient pricingClient;
    private final CartMapper mapper;

    @Value("${cart.cache.enabled:true}")
    private boolean cacheEnabled;

    public Cart getOrCreateCart(String sessionId) {
        return cacheService.getCart(sessionId)
                .orElseGet(() -> {
                    Cart cart = Cart.create(sessionId);
                    return cacheService.saveCart(cart);
                });
    }

    public Cart getCartEntity(String sessionId) {
        return getOrCreateCart(sessionId); // ya maneja creación
    }

    @CircuitBreaker(name = "inventoryClient", fallbackMethod = "inventoryFallback")
    @Retry(name = "inventoryClient")
    public Cart addItem(String sessionId, AddItemRequestDTO req) {
        Cart cart = getCartEntity(sessionId);

        // Verificar stock
        if (!Boolean.TRUE.equals(inventoryClient.checkStock(req.getProductId(), req.getQuantity()))) {
            throw new RuntimeException("Stock insuficiente");
        }

        ProductDTO product = productClient.getProductById(req.getProductId());

        // Buscar si ya existe → sumar cantidad
        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(req.getProductId()))
                .findFirst();

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + req.getQuantity());
        } else {
            CartItem item = new CartItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setPrice(product.getPrice());
            item.setQuantity(req.getQuantity());
            cart.getItems().add(item);
        }

        return saveCart(cart);
    }

    public Cart updateItem(String sessionId, UpdateItemRequestDTO req) {
        Cart cart = getCartEntity(sessionId);
        cart.updateQuantity(req.getProductId(), req.getQuantity());
        return saveCart(cart);
    }


    public Cart removeItem(String sessionId, String productId) {
        Cart cart = getCartEntity(sessionId);
        cart.getItems().removeIf(i -> i.getProductId().equals(productId));

        if (cacheEnabled) {
            return cacheService.saveCart(cart);
        } else {
            return repository.save(cart);
        }
    }

    private Cart saveCart(Cart cart) {
        if (cacheEnabled) {
            return cacheService.saveCart(cart);
        } else {
            return repository.save(cart);
        }
    }

    public Cart inventoryFallback(
        String sessionId,
        AddItemRequestDTO req,
        Throwable t
) {
    // Recuperar el carrito actual
    Cart cart = getOrCreateCart(sessionId);
    return cart;
}


    // Nuevo: cálculo completo con pricing-service
    public CartResponseDTO toFullResponse(Cart cart) {
        // Preparar request para pricing
        PriceCalculationRequest pricingReq = new PriceCalculationRequest();
        pricingReq.setItems(
                cart.getItems().stream().map(i -> {
                    var item = new PriceCalculationRequest.CartItem();
                    item.setProductId(i.getProductId());
                    item.setQuantity(i.getQuantity());
                    return item;
                }).toList()
        );

        PriceCalculationResponse pricing = pricingClient.calculatePrice(pricingReq);

        CartResponseDTO response = mapper.toResponse(cart);

        // Enriquecer con pricing real
        response.setSubtotal(pricing.getSubtotal().doubleValue());
        response.setDiscount(pricing.getDiscountAmount().doubleValue());
        response.setTax(pricing.getTaxAmount().doubleValue());
        response.setShippingEstimate(pricing.getShippingAmount().doubleValue());
        response.setTotal(pricing.getTotal().doubleValue());

        // Puedes mapear también los items con precios unitarios descontados si lo deseas

        return response;
    }
}