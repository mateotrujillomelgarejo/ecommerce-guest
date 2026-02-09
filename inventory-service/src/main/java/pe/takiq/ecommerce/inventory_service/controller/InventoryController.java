package pe.takiq.ecommerce.inventory_service.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pe.takiq.ecommerce.inventory_service.model.Inventory;
import pe.takiq.ecommerce.inventory_service.repository.InventoryRepository;
import pe.takiq.ecommerce.inventory_service.service.InventoryService;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService service;
    private final InventoryRepository repo;

    @PostMapping("/save")
    public Inventory guardar(@RequestBody Inventory entity) {
        return repo.save(entity);
    }
    

    @GetMapping("/{productId}/check")
    public ResponseEntity<Boolean> check(
            @PathVariable("productId") String productId,
            @RequestParam("quantity") int quantity) {
        return ResponseEntity.ok(service.checkAvailability(productId, quantity));
    }

    @PostMapping("/check-batch")
    public ResponseEntity<Boolean> checkBatch(@RequestBody Map<String, Integer> items) {
        return ResponseEntity.ok(service.checkMultipleAvailability(items));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Void> update(
            @PathVariable("productId") String productId,
            @RequestParam("quantity") int quantity) {
        service.updateStock(productId, quantity);
        return ResponseEntity.ok().build();
    }
}
