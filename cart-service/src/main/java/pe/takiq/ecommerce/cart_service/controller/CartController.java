package pe.takiq.ecommerce.cart_service.controller;

import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import pe.takiq.ecommerce.cart_service.dto.request.AddItemRequestDTO;
import pe.takiq.ecommerce.cart_service.dto.response.CartResponseDTO;
import pe.takiq.ecommerce.cart_service.mapper.CartMapper;
import pe.takiq.ecommerce.cart_service.service.CartService;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService service;
    private final CartMapper mapper;

    @PostMapping
    public CartResponseDTO createCart() {
        return mapper.toResponse(service.createCart());
    }

    @GetMapping("/{cartId}")
    public CartResponseDTO getCart(
            @PathVariable("cartId") String cartId) {
        return mapper.toResponse(service.getCartEntity(cartId));
    }

    @PostMapping("/{cartId}/items")
    public CartResponseDTO addItem(
            @PathVariable("cartId") String cartId,
            @RequestBody AddItemRequestDTO request) {
        return mapper.toResponse(service.addItem(cartId, request));
    }

    @DeleteMapping("/{cartId}/items/{productId}")
    public CartResponseDTO removeItem(
            @PathVariable("cartId") String cartId,
            @PathVariable("productId") String productId) {
        return mapper.toResponse(service.removeItem(cartId, productId));
    }
}
