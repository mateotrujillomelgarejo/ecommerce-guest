package pe.takiq.ecommerce.cart_service.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartCacheService cacheService;
    private final InventoryClient inventoryClient;
    private final ProductClient productClient;
    private final PricingClient pricingClient;
    private final CartMapper mapper;

    public Cart getOrCreateCart(String sessionId) {
        return cacheService.getCart(sessionId)
                .orElseGet(() -> {
                    Cart cart = Cart.create(sessionId);
                    return cacheService.saveCart(cart);
                });
    }

    public Cart addItem(String sessionId, AddItemRequestDTO req) {
        Cart cart = getOrCreateCart(sessionId);

        if (!checkStockResilient(req.getProductId(), req.getQuantity())) {
            log.warn("Stock insuficiente o servicio Inventory no disponible para el producto: {}", req.getProductId());
            return cart; 
        }

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(req.getProductId()))
                .findFirst();

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + req.getQuantity());
        } else {
            try {
                ProductDTO product = getProductResilient(req.getProductId());
                CartItem item = new CartItem();
                item.setProductId(product.getId());
                item.setProductName(product.getName());
                // OPTIMIZACIÓN LOGRADA: Snapshot local guardado en caché para evitar consultas futuras
                item.setPrice(product.getPrice()); 
                item.setQuantity(req.getQuantity());
                cart.getItems().add(item);
            } catch (Exception e) {
                log.error("Product Service inalcanzable. Omitiendo agregado de item.", e);
            }
        }

        return cacheService.saveCart(cart);
    }

    public Cart updateItem(String sessionId, UpdateItemRequestDTO req) {
        Cart cart = getOrCreateCart(sessionId);

        if (req.getQuantity() > 0) {
            if (!checkStockResilient(req.getProductId(), req.getQuantity())) {
                log.warn("Stock insuficiente o servicio no disponible para actualizar: {}", req.getProductId());
                return cart;
            }
        }

        cart.updateQuantity(req.getProductId(), req.getQuantity());
        return cacheService.saveCart(cart);
    }

    public Cart removeItem(String sessionId, String productId) {
        Cart cart = getOrCreateCart(sessionId);
        cart.getItems().removeIf(i -> i.getProductId().equals(productId));
        return cacheService.saveCart(cart);
    }

    public CartResponseDTO toFullResponse(Cart cart) {
        CartResponseDTO response = mapper.toResponse(cart);

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            response.setSubtotal(0.0);
            response.setDiscount(0.0);
            response.setTax(0.0);
            response.setShippingEstimate(0.0);
            response.setTotal(0.0);
            return response;
        }

        PriceCalculationRequest pricingReq = new PriceCalculationRequest();
        pricingReq.setItems(
            cart.getItems().stream().map(i -> {
                var item = new PriceCalculationRequest.CartItem();
                item.setProductId(i.getProductId());
                item.setQuantity(i.getQuantity());
                return item;
            }).toList()
        );

        try {
            PriceCalculationResponse pricing = calculatePricingResilient(pricingReq);
            response.setSubtotal(pricing.getSubtotal().doubleValue());
            response.setDiscount(pricing.getDiscountAmount().doubleValue());
            response.setTax(pricing.getTaxAmount().doubleValue());
            response.setShippingEstimate(pricing.getShippingAmount().doubleValue());
            response.setTotal(pricing.getTotal().doubleValue());
        } catch (Exception e) {
            log.warn("Pricing Service inalcanzable. Calculando precios base localmente usando el Snapshot de Redis.");
            // OPTIMIZACIÓN APLICADA: Si Pricing falla, usamos el Snapshot garantizando continuidad del negocio.
            double fallbackSubtotal = cart.getItems().stream()
                    .mapToDouble(i -> (i.getPrice() != null ? i.getPrice() : 0.0) * i.getQuantity())
                    .sum();
            
            response.setSubtotal(fallbackSubtotal);
            response.setDiscount(0.0);
            response.setTax(fallbackSubtotal * 0.18);
            response.setShippingEstimate(0.0);
            response.setTotal(fallbackSubtotal + (fallbackSubtotal * 0.18));
        }

        return response;
    }

    // --- MÉTODOS DE RESILIENCIA AISLADOS ---

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "checkStockFallback")
    private Boolean checkStockResilient(String productId, Integer quantity) {
        return inventoryClient.checkStock(productId, quantity);
    }

    private Boolean checkStockFallback(String productId, Integer quantity, Throwable t) {
        log.error("Fallback activado: Inventory-Service inalcanzable. Bloqueando adición.");
        return false;
    }

    @Retry(name = "productService")
    private ProductDTO getProductResilient(String productId) {
        return productClient.getProduct(productId);
    }

    @CircuitBreaker(name = "pricingService")
    private PriceCalculationResponse calculatePricingResilient(PriceCalculationRequest request) {
        return pricingClient.calculate(request);
    }

    // --- WRAPPERS ---

    public CartResponseDTO addItemFull(String sessionId, AddItemRequestDTO req) {
        Cart cart = addItem(sessionId, req);
        return toFullResponse(cart);
    }

    public CartResponseDTO updateItemFull(String sessionId, UpdateItemRequestDTO req) {
        Cart cart = updateItem(sessionId, req);
        return toFullResponse(cart);
    }

    public CartResponseDTO removeItemFull(String sessionId, String productId) {
        Cart cart = removeItem(sessionId, productId);
        return toFullResponse(cart);
    }

    public CartResponseDTO getCartFull(String sessionId) {
        Cart cart = getOrCreateCart(sessionId);
        return toFullResponse(cart);
    }
}