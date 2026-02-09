package pe.takiq.ecommerce.pricing_service.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

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