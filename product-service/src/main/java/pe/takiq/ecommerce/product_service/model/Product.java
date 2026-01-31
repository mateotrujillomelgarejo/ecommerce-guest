package pe.takiq.ecommerce.product_service.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "products")
public class Product {
    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private String description;
    private Double price;

    @Indexed
    private String category;

    private List<String> images;

    private Double averageRating;

    private List<String> tags;
}