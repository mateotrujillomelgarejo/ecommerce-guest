package pe.takiq.ecommerce.review_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "reviews",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_review_user_product",
        columnNames = {"user_id", "product_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID reviewId;

    @Column(nullable = false)
    private String productId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "guest_email")
    private String guestEmail;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(nullable = false)
    private boolean verified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status;

    @Column(nullable = false)
    private int helpfulCount;

    @Column(length = 500)
    private String rejectionReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (reviewId == null) reviewId = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = ReviewStatus.PENDING;
        if (helpfulCount < 0) helpfulCount = 0;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}