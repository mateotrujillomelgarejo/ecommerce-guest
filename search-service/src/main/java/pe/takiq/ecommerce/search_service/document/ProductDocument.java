package pe.takiq.ecommerce.search_service.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document(indexName = "products")
@Setting(settingPath = "/elasticsearch/settings.json")
@Mapping(mappingPath = "/elasticsearch/mappings.json")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

    @Id
    private String productId;

    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "spanish"),
        otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword),
            @InnerField(suffix = "suggest", type = FieldType.Search_As_You_Type)
        }
    )
    private String name;

    @Field(type = FieldType.Text, analyzer = "spanish")
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String subcategory;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal price;

    @Field(type = FieldType.Float)
    private Double averageRating;

    @Field(type = FieldType.Integer)
    private Integer reviewCount;

    @Field(type = FieldType.Keyword)
    private String imageUrl;

    @Field(type = FieldType.Boolean)
    private boolean active;

    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant updatedAt;
}