package pe.takiq.ecommerce.cart_service.dto; 
import lombok.Data; 

@Data 
public class ProductDTO { 
    private String id; 
    private String name; 
    private String description; 
    private Double price; 
    private Integer stock; 
}