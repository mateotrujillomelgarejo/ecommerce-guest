package pe.takiq.ecommerce.payment_service.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions", uniqueConstraints = @UniqueConstraint(columnNames = "paymentId"))
@Data
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String paymentId;           // Idempotency key (de gateway o generado)

    private String orderId;
    private BigDecimal amount;
    private String status;              // PENDING, SUCCESS, FAILED, REFUNDED
    private String gateway;             // "STRIPE", "MERCADO_PAGO", "SIMULATED"
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}