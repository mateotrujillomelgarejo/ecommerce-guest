package pe.takiq.ecommerce.customer_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.takiq.ecommerce.customer_service.model.Customer;
import pe.takiq.ecommerce.customer_service.service.CustomerService;

@RestController
@RequestMapping("/guests")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService service;

    @PostMapping
    public ResponseEntity<Customer> createGuest(@RequestBody Customer request) {
        Customer created = service.createGuest(
                request.getEmail(),
                request.getName(),
                request.getPhone()
        );
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getGuest(@PathVariable("id") String id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Customer> updateGuest(@PathVariable String id, @RequestBody Customer update) {
        return ResponseEntity.ok(service.update(id, update));
    }

    @GetMapping("/search")
    public ResponseEntity<Customer> searchByEmail(@RequestParam String email) {
        return service.findByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}