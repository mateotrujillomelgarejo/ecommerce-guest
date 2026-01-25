package pe.takiq.ecommerce.inventory_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Inventory {
    @Id
    private String productId;
    private Integer stock;
}