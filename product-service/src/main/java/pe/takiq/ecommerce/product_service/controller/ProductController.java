package pe.takiq.ecommerce.product_service.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import pe.takiq.ecommerce.product_service.dto.ProductDetailDTO;
import pe.takiq.ecommerce.product_service.model.Product;
import pe.takiq.ecommerce.product_service.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

@GetMapping
public Page<Product> getAll(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "name,asc") String sort
) {
    String[] sortParams = sort.split(",");
    Sort.Direction direction = Sort.Direction.fromString(sortParams[1]);

    Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(direction, sortParams[0])
    );

    return service.findAll(pageable);
}


    @GetMapping("/{id}")
    public Product getById(@PathVariable String id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/details")
    public ProductDetailDTO getDetails(@PathVariable String id, @RequestParam(required = false) Integer quantity) {
        return service.getDetails(id, quantity);
    }

    @PostMapping
    public Product create(@RequestBody Product product) {
        return service.save(product);
    }

    // Nueva: Búsqueda
    @GetMapping("/search")
    public List<Product> search(@RequestParam String name) {
        return service.searchByName(name);
    }

    // Nueva: Filtrado por categoría
    @GetMapping("/category/{category}")
    public Page<Product> filterByCategory(@PathVariable String category,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return service.filterByCategory(category, PageRequest.of(page, size));
    }

    // Nueva: Filtrado por precio
    @GetMapping("/price-range")
    public Page<Product> filterByPrice(@RequestParam Double min, @RequestParam Double max,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        return service.filterByPrice(min, max, PageRequest.of(page, size));
    }

    // Nueva: Populares
    @GetMapping("/popular")
    public Page<Product> getPopular(@RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        return service.getPopular(PageRequest.of(page, size));
    }
}