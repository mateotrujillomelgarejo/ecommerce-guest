package pe.takiq.ecommerce.product_service.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdatedEvent {
    private String productId;
    private String name;
    private BigDecimal price;
    private boolean active;
}