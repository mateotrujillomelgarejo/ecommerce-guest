package pe.takiq.ecommerce.product_service.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.List;

@Data
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private String description;

    private BigDecimal price;

    @Indexed
    private String category;

    private String subcategory;

    private List<String> images;

    private List<String> tags;

    @Indexed(unique = true, sparse = true)
    private String sku;

    private Double averageRating;

    private Integer reviewCount;

    private String metaTitle;
    private String metaDescription;
    private String slug;

    private boolean active = true;
}