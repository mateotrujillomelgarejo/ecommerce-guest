package pe.takiq.ecommerce.inventory_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.takiq.ecommerce.inventory_service.dto.ReserveStockRequest;
import pe.takiq.ecommerce.inventory_service.dto.RestockRequest;
import pe.takiq.ecommerce.inventory_service.model.Inventory;
import pe.takiq.ecommerce.inventory_service.repository.InventoryRepository;
import pe.takiq.ecommerce.inventory_service.service.InventoryService;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService service;
    private final InventoryRepository repo;

    @GetMapping("/{productId}/check")
    public ResponseEntity<Boolean> check(
            @PathVariable("productId") String productId,
            @RequestParam("quantity") int quantity) {
        return ResponseEntity.ok(service.checkAvailability(productId, quantity));
    }

    @PostMapping("/reserve")
    public ResponseEntity<Void> reserveStock(@RequestBody ReserveStockRequest request) {
        service.reserveStock(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/restock")
    public ResponseEntity<Inventory> restock(@Valid @RequestBody RestockRequest request) {
        return ResponseEntity.ok(service.restock(request));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Void> update(
            @PathVariable("productId") String productId,
            @RequestParam("quantity") int quantity) {
        service.updateStock(productId, quantity);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<Inventory> create(@RequestBody Inventory entity) {
        Inventory saved = repo.save(entity);
        service.updateStock(saved.getProductId(), saved.getAvailableQuantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Inventory>> getAllInventory() {
        return ResponseEntity.ok(service.getAllInventory());
    }
}