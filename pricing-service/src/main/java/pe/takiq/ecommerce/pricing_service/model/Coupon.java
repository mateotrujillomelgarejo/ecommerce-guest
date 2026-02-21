package pe.takiq.ecommerce.pricing_service.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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
    private String code;
    private BigDecimal discountAmount;
    private BigDecimal discountPercent;
    private String discountType;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private boolean active = true;
    private Integer maxUses;
    private Integer usesCount = 0;
}