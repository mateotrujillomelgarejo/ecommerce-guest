package pe.takiq.ecommerce.pricing_service.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "coupons")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;  // Ej: "VERANO25"

    private BigDecimal discountAmount;  // Fijo (ej: 10.00)

    private BigDecimal discountPercent;  // Porcentaje (ej: 0.25 para 25%)

    private String discountType;  // "FIXED", "PERCENT", "PER_PRODUCT"

    private LocalDateTime validFrom;
    private LocalDateTime validUntil;

    private boolean active = true;

    private Integer maxUses;  // Límite global (opcional)

    private Integer usesCount = 0;
}