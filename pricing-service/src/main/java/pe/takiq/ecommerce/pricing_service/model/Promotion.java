package pe.takiq.ecommerce.pricing_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String productId;  // Opcional: promoción por producto

    private String category;   // O por categoría

    private BigDecimal discountPercent;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private boolean active = true;
}