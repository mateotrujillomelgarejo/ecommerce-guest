package pe.takiq.ecommerce.inventory_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pe.takiq.ecommerce.inventory_service.model.Inventory;
import pe.takiq.ecommerce.inventory_service.service.InventoryService;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService service;

    @PostMapping
    public Inventory create(@RequestBody Inventory inventory) {
        return service.save(inventory);
    }

@GetMapping("/{productId}/check")
public ResponseEntity<Boolean> checkStock(
        @PathVariable("productId") String productId,
        @RequestParam("quantity") int quantity) {

    return ResponseEntity.ok(service.checkStock(productId, quantity));
}

}