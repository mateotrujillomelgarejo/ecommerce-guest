package pe.takiq.ecommerce.review_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.takiq.ecommerce.review_service.dto.request.CreateReviewRequest;
import pe.takiq.ecommerce.review_service.dto.response.RatingStatsResponse;
import pe.takiq.ecommerce.review_service.dto.response.ReviewResponse;
import pe.takiq.ecommerce.review_service.entity.Review;
import pe.takiq.ecommerce.review_service.entity.ReviewStatus;
import pe.takiq.ecommerce.review_service.event.ProductRatingUpdatedEvent;
import pe.takiq.ecommerce.review_service.exception.ReviewException;
import pe.takiq.ecommerce.review_service.repository.ReviewRepository;
import pe.takiq.ecommerce.review_service.service.PurchaseVerificationService;
import pe.takiq.ecommerce.review_service.service.ReviewService;

import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private final PurchaseVerificationService purchaseVerificationService;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.product-rating-updated}")
    private String ratingUpdatedRoutingKey;

    @Value("${review.cache.rating-ttl-seconds}")
    private long ratingCacheTtl;

    private static final String RATING_CACHE_PREFIX = "review:rating:";

    // ─────────────────────────────────────────────────────────────────────────
    // CREAR RESEÑA
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReviewResponse createReview(String userId, CreateReviewRequest request) {
        if (reviewRepository.existsByProductIdAndUserId(request.getProductId(), userId)) {
            throw new ReviewException(
                "Ya tienes una reseña para este producto", HttpStatus.CONFLICT);
        }

        boolean verified = purchaseVerificationService.verifyPurchase(userId, request.getProductId());

        if (!verified) {
            log.info("Reseña creada sin verificación de compra: userId={}, productId={}",
                    userId, request.getProductId());
        }

        Review review = Review.builder()
                .productId(request.getProductId())
                .userId(userId)
                .rating(request.getRating())
                .title(request.getTitle().trim())
                .body(request.getBody().trim())
                .verified(verified)
                .status(ReviewStatus.PENDING)
                .helpfulCount(0)
                .build();

        review = reviewRepository.save(review);
        log.info("Reseña creada: reviewId={}, productId={}, userId={}, verified={}",
                review.getReviewId(), review.getProductId(), userId, verified);

        return toResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponse markHelpful(UUID reviewId, String userId) {
        Review review = findReviewOrThrow(reviewId);

        if (review.getStatus() != ReviewStatus.APPROVED) {
            throw new ReviewException(
                "Solo se pueden marcar como útiles reseñas aprobadas", HttpStatus.BAD_REQUEST);
        }

        if (userId.equals(review.getUserId())) {
            throw new ReviewException(
                "No puedes marcar tu propia reseña como útil", HttpStatus.BAD_REQUEST);
        }

        review.setHelpfulCount(review.getHelpfulCount() + 1);
        return toResponse(reviewRepository.save(review));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LECTURA
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getApprovedReviewsByProduct(
            String productId, Pageable pageable) {
        return reviewRepository
                .findByProductIdAndStatus(productId, ReviewStatus.APPROVED, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RatingStatsResponse getRatingStats(String productId) {
        String cacheKey = RATING_CACHE_PREFIX + productId;

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                RatingStatsResponse cachedStats = objectMapper.convertValue(
                        cached, RatingStatsResponse.class);
                cachedStats.setCached(true);
                return cachedStats;
            }
        } catch (Exception e) {
            log.warn("Error leyendo caché de rating para productId={}: {}", productId, e.getMessage());
        }

        Double avg = reviewRepository.calculateAverageRating(productId);
        long count = reviewRepository.countByProductIdAndStatus(
                productId, ReviewStatus.APPROVED);

        RatingStatsResponse stats = RatingStatsResponse.builder()
                .productId(productId)
                .averageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0)
                .reviewCount(count)
                .cached(false)
                .build();

        redisTemplate.opsForValue().set(cacheKey, stats, ratingCacheTtl, TimeUnit.SECONDS);
        return stats;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ADMIN
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReviewResponse approveReview(UUID reviewId) {
        Review review = findReviewOrThrow(reviewId);

        if (review.getStatus() == ReviewStatus.APPROVED) {
            throw new ReviewException("La reseña ya está aprobada", HttpStatus.CONFLICT);
        }

        review.setStatus(ReviewStatus.APPROVED);
        review.setRejectionReason(null);
        review = reviewRepository.save(review);

        recalculateAndPublishRating(review.getProductId());
        log.info("Reseña aprobada: reviewId={}, productId={}", reviewId, review.getProductId());

        return toResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponse rejectReview(UUID reviewId, String reason) {
        Review review = findReviewOrThrow(reviewId);

        boolean wasApproved = review.getStatus() == ReviewStatus.APPROVED;

        review.setStatus(ReviewStatus.REJECTED);
        review.setRejectionReason(reason);
        review = reviewRepository.save(review);

        if (wasApproved) {
            recalculateAndPublishRating(review.getProductId());
        }

        log.info("Reseña rechazada: reviewId={}, productId={}, reason={}",
                reviewId, review.getProductId(), reason);

        return toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getPendingReviews(Pageable pageable) {
        return reviewRepository.findByStatus(ReviewStatus.PENDING, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getAllReviewsByProduct(String productId, Pageable pageable) {
        return reviewRepository.findByProductId(productId, pageable).map(this::toResponse);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS PRIVADOS
    // ─────────────────────────────────────────────────────────────────────────

    private void recalculateAndPublishRating(String productId) {
        Double avg = reviewRepository.calculateAverageRating(productId);
        long count = reviewRepository.countByProductIdAndStatus(
                productId, ReviewStatus.APPROVED);

        double roundedAvg = avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;

        // Actualizar Redis
        String cacheKey = RATING_CACHE_PREFIX + productId;
        RatingStatsResponse stats = RatingStatsResponse.builder()
                .productId(productId)
                .averageRating(roundedAvg)
                .reviewCount(count)
                .cached(false)
                .build();
        redisTemplate.opsForValue().set(cacheKey, stats, ratingCacheTtl, TimeUnit.SECONDS);

        // Publicar evento para Product Service y Search Service
        ProductRatingUpdatedEvent event = ProductRatingUpdatedEvent.builder()
                .productId(productId)
                .averageRating(roundedAvg)
                .reviewCount(count)
                .updatedAt(Instant.now())
                .build();

        rabbitTemplate.convertAndSend(exchange, ratingUpdatedRoutingKey, event);
        log.info("ProductRatingUpdatedEvent publicado: productId={}, avg={}, count={}",
                productId, roundedAvg, count);
    }

    private Review findReviewOrThrow(UUID reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ReviewException(
                    "Reseña no encontrada: " + reviewId, HttpStatus.NOT_FOUND));
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .productId(review.getProductId())
                .userId(review.getUserId())
                .rating(review.getRating())
                .title(review.getTitle())
                .body(review.getBody())
                .verified(review.isVerified())
                .status(review.getStatus())
                .helpfulCount(review.getHelpfulCount())
                .rejectionReason(review.getRejectionReason())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}