package pe.takiq.ecommerce.order_service.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Data;

@Entity
@Data
@Table(
    name = "orders",
    indexes = {
        @Index(
            name = "idx_orders_payment_id",
            columnList = "paymentId",
            unique = true
        ),
        @Index(
            name = "idx_orders_guest_email",
            columnList = "guestId,email"
        ),
        @Index(
            name = "idx_orders_status",
            columnList = "status"
        )
    }
)
public class Order {

    /* =========================
       IDENTIDAD
       ========================= */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String guestId;
    private String email;
    private String sessionId;

    /* =========================
       ESTADO
       ========================= */
    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.CREATED;

    /* =========================
       TRAZABILIDAD / AUDITORÍA
       ========================= */
    private String createdBy = "system:guest-checkout";
    private String lastModifiedBy;

    /* =========================
       PAGO / ENVÍO
       ========================= */
    private String paymentId;
    private String shippingId;

    private BigDecimal totalAmount;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal tax;
    private BigDecimal shippingCost;

    /* =========================
       ITEMS
       ========================= */
    @OneToMany(
        mappedBy = "order",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<OrderItem> items = new ArrayList<>();

    /* =========================
       TIMESTAMPS
       ========================= */
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;

    private String cancellationReason;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /* =========================
       ENUM DE ESTADOS
       ========================= */
    public enum OrderStatus {
        CREATED,
        PAYMENT_PENDING,
        PAID,
        CONFIRMED,
        SHIPPED,
        DELIVERED,
        CANCELLED,
        FAILED
    }

    /* =========================
       TRANSICIONES SEGURAS
       ========================= */
    public void transitionTo(OrderStatus newStatus, String reason) {

        if (!isValidTransition(newStatus)) {
            throw new IllegalStateException(
                "Transición inválida de " + status + " a " + newStatus
            );
        }

        this.status = newStatus;

        switch (newStatus) {
            case PAID -> this.paidAt = LocalDateTime.now();
            case SHIPPED -> this.shippedAt = LocalDateTime.now();
            case DELIVERED -> this.deliveredAt = LocalDateTime.now();
            case CANCELLED -> {
                this.cancelledAt = LocalDateTime.now();
                this.cancellationReason = reason;
            }
            default -> {}
        }
    }

    private boolean isValidTransition(OrderStatus target) {
        return switch (status) {
            case CREATED, PAYMENT_PENDING ->
                target == OrderStatus.PAID ||
                target == OrderStatus.CANCELLED ||
                target == OrderStatus.FAILED;

            case PAID ->
                target == OrderStatus.CONFIRMED ||
                target == OrderStatus.CANCELLED;

            case CONFIRMED ->
                target == OrderStatus.SHIPPED ||
                target == OrderStatus.CANCELLED;

            case SHIPPED ->
                target == OrderStatus.DELIVERED ||
                target == OrderStatus.CANCELLED;

            case DELIVERED, CANCELLED, FAILED -> false;
        };
    }
}
