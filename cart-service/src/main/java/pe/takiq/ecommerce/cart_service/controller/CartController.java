package pe.takiq.ecommerce.cart_service.controller;

import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import pe.takiq.ecommerce.cart_service.dto.request.AddItemRequestDTO;
import pe.takiq.ecommerce.cart_service.dto.request.UpdateItemRequestDTO;
import pe.takiq.ecommerce.cart_service.dto.response.CartResponseDTO;
import pe.takiq.ecommerce.cart_service.mapper.CartMapper;
import pe.takiq.ecommerce.cart_service.service.CartService;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService service;
    private final CartMapper mapper;

    // Crear o recuperar carrito por sessionId
    @PostMapping
    public CartResponseDTO getOrCreateCart(@RequestParam("sessionId") String sessionId) {
        return mapper.toResponse(service.getOrCreateCart(sessionId));
    }

    // Obtener carrito (flujo paso 3.1)
    @GetMapping
    public CartResponseDTO getCart(@RequestParam("sessionId") String sessionId) {
        return mapper.toResponse(service.getCartEntity(sessionId));
    }

    // Agregar ítem (flujo 2.1)
    @PostMapping("/items")
    public CartResponseDTO addItem(
            @RequestParam("sessionId") String sessionId,
            @RequestBody AddItemRequestDTO request) {
        return mapper.toResponse(service.addItem(sessionId, request));
    }

    // Actualizar cantidad o eliminar si quantity=0
    @PatchMapping("/items")
    public CartResponseDTO updateItem(
            @RequestParam("sessionId") String sessionId,
            @RequestBody UpdateItemRequestDTO request) {
        return mapper.toResponse(service.updateItem(sessionId, request));
    }

    // Eliminar ítem específico
    @DeleteMapping("/items/{productId}")
    public CartResponseDTO removeItem(
            @RequestParam("sessionId") String sessionId,
            @PathVariable("productId") String productId) {
        return mapper.toResponse(service.removeItem(sessionId, productId));
    }
}