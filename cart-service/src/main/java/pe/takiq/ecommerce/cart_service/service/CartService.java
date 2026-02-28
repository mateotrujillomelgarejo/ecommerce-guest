package pe.takiq.ecommerce.cart_service.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pe.takiq.ecommerce.cart_service.client.CartIntegrationManager;
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
    private final CartIntegrationManager integrationManager;
    private final CartMapper mapper;

    public Cart getOrCreateCart(String sessionId) {
        return cacheService.getCart(sessionId)
                .orElseGet(() -> cacheService.saveCart(Cart.create(sessionId)));
    }

    public CartResponseDTO getCartFull(String sessionId) {
        Cart cart = getOrCreateCart(sessionId);
        
        if (!cart.isNeedsRecalculation() && !cart.getItems().isEmpty()) {
            return mapper.toResponse(cart);
        }
        return toFullResponse(cart);
    }

    public CartResponseDTO addItemFull(String sessionId, AddItemRequestDTO req) {
        Cart cart = getOrCreateCart(sessionId);

        if (!integrationManager.checkStock(req.getProductId(), req.getQuantity())) {
            log.warn("Stock insuficiente para el producto: {}", req.getProductId());
            return mapper.toResponse(cart); 
        }

        Optional<CartItem> existing = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(req.getProductId()))
                .findFirst();

        if (existing.isPresent()) {
            cart.updateQuantity(req.getProductId(), existing.get().getQuantity() + req.getQuantity());
        } else {
            try {

                ProductDTO product = integrationManager.getProduct(req.getProductId());

                String principalImage = (product.getImages() != null && !product.getImages().isEmpty()) 
                        ? product.getImages().get(0) 
                        : null;

                CartItem item = new CartItem(
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        req.getQuantity(),
                        principalImage
                );
                cart.addItem(item);
            } catch (Exception e) {
                log.error("Product-Service inalcanzable. Bloqueando agregado de item.", e);
                throw new RuntimeException("No se puede validar el producto en este momento.");
            }
        }

        cart = cacheService.saveCart(cart);
        return toFullResponse(cart);
    }

    public CartResponseDTO updateItemFull(String sessionId, UpdateItemRequestDTO req) {
        Cart cart = getOrCreateCart(sessionId);

        if (req.getQuantity() > 0) {
            if (!integrationManager.checkStock(req.getProductId(), req.getQuantity())) {
                log.warn("Stock insuficiente para actualizar: {}", req.getProductId());
                return mapper.toResponse(cart);
            }
        }

        cart.updateQuantity(req.getProductId(), req.getQuantity());
        cart = cacheService.saveCart(cart);
        return toFullResponse(cart);
    }

    public CartResponseDTO removeItemFull(String sessionId, String productId) {
        Cart cart = getOrCreateCart(sessionId);
        cart.removeItem(productId);
        cart = cacheService.saveCart(cart);
        return toFullResponse(cart);
    }


    private CartResponseDTO toFullResponse(Cart cart) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            cart.setSubtotal(0.0);
            cart.setDiscount(0.0);
            cart.setTax(0.0);
            cart.setShippingEstimate(0.0);
            cart.setTotal(0.0);
            cart.setNeedsRecalculation(false);
            cacheService.saveCart(cart);
            return mapper.toResponse(cart);
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
            PriceCalculationResponse pricing = integrationManager.calculatePricing(pricingReq);
            cart.setSubtotal(pricing.getSubtotal().doubleValue());
            cart.setDiscount(pricing.getDiscountAmount().doubleValue());
            cart.setTax(pricing.getTaxAmount().doubleValue());
            cart.setShippingEstimate(pricing.getShippingAmount().doubleValue());
            cart.setTotal(pricing.getTotal().doubleValue());
            cart.setNeedsRecalculation(false);
        } catch (Exception e) {
            log.warn("Pricing-Service inalcanzable. Calculando precios de Fallback localmente usando Snapshot de Redis.");
            
            double fallbackTotal = cart.getItems().stream()
                    .mapToDouble(i -> (i.getPrice() != null ? i.getPrice() : 0.0) * i.getQuantity())
                    .sum();
            
            cart.setSubtotal(fallbackTotal);
            cart.setDiscount(0.0);
            cart.setTax(0.0); 
            cart.setShippingEstimate(0.0);
            cart.setTotal(fallbackTotal);
        }

        cacheService.saveCart(cart);
        return mapper.toResponse(cart);
    }
}