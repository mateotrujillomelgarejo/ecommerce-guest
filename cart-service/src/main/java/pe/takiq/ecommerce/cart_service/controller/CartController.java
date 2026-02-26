package pe.takiq.ecommerce.cart_service.controller;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import pe.takiq.ecommerce.cart_service.dto.request.AddItemRequestDTO;
import pe.takiq.ecommerce.cart_service.dto.request.UpdateItemRequestDTO;
import pe.takiq.ecommerce.cart_service.dto.response.CartResponseDTO;
import pe.takiq.ecommerce.cart_service.service.CartService;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService service;

    @PostMapping
    public CartResponseDTO getOrCreateCart(@RequestParam("sessionId") String sessionId) {
        return service.getCartFull(sessionId);
    }

    @GetMapping
    public CartResponseDTO getCart(@RequestParam("sessionId") String sessionId) {
        return service.getCartFull(sessionId);
    }

    @PostMapping("/calculate-totals")
    public CartResponseDTO calculateTotals(@RequestParam("sessionId") String sessionId) {
        return service.getCartFull(sessionId);
    }

    @PostMapping("/items")
    public CartResponseDTO addItem(
            @RequestParam("sessionId") String sessionId,
            @RequestBody AddItemRequestDTO request) {
        return service.addItemFull(sessionId, request);
    }

    @PatchMapping("/items")
    public CartResponseDTO updateItem(
            @RequestParam("sessionId") String sessionId,
            @RequestBody UpdateItemRequestDTO request) {
        return service.updateItemFull(sessionId, request);
    }

    @DeleteMapping("/items/{productId}")
    public CartResponseDTO removeItem(
            @RequestParam("sessionId") String sessionId,
            @PathVariable("productId") String productId) {
        return service.removeItemFull(sessionId, productId);
    }
}