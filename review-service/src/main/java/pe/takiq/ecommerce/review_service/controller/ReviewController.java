package pe.takiq.ecommerce.review_service.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.takiq.ecommerce.review_service.dto.request.CreateReviewRequest;
import pe.takiq.ecommerce.review_service.dto.response.ApiResponse;
import pe.takiq.ecommerce.review_service.dto.response.RatingStatsResponse;
import pe.takiq.ecommerce.review_service.dto.response.ReviewResponse;
import pe.takiq.ecommerce.review_service.service.ReviewService;

import java.util.UUID;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;


    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getProductReviews(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "NEWEST") SortOption sort) {

        Pageable pageable = buildPageable(page, size, sort);
        Page<ReviewResponse> reviews =
                reviewService.getApprovedReviewsByProduct(productId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(reviews));
    }


    @GetMapping("/product/{productId}/stats")
    public ResponseEntity<ApiResponse<RatingStatsResponse>> getRatingStats(
            @PathVariable String productId) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getRatingStats(productId)));
    }


    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody CreateReviewRequest request) {

        ReviewResponse review = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Reseña enviada. Será visible tras ser aprobada.", review));
    }


    @PostMapping("/{reviewId}/helpful")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> markHelpful(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal String userId) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Marcada como útil", reviewService.markHelpful(reviewId, userId)));
    }


    @GetMapping("/admin/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getPendingReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.ASC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getPendingReviews(pageable)));
    }

    @PutMapping("/{reviewId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReviewResponse>> approveReview(
            @PathVariable UUID reviewId) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Reseña aprobada", reviewService.approveReview(reviewId)));
    }


    @PutMapping("/{reviewId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReviewResponse>> rejectReview(
            @PathVariable UUID reviewId,
            @RequestParam @Size(min = 5, max = 500) String reason) {

        return ResponseEntity.ok(ApiResponse.ok(
                "Reseña rechazada", reviewService.rejectReview(reviewId, reason)));
    }


    @GetMapping("/admin/product/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getAllProductReviews(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.ok(
                reviewService.getAllReviewsByProduct(productId, pageable)));
    }

    private Pageable buildPageable(int page, int size, SortOption sort) {
        Sort sorting = switch (sort) {
            case NEWEST    -> Sort.by(Sort.Direction.DESC, "createdAt");
            case HELPFUL   -> Sort.by(Sort.Direction.DESC, "helpfulCount");
            case RATING_HIGH -> Sort.by(Sort.Direction.DESC, "rating");
            case RATING_LOW  -> Sort.by(Sort.Direction.ASC, "rating");
        };
        return PageRequest.of(
                Math.max(0, page),
                Math.min(Math.max(1, size), 50),
                sorting
        );
    }

    public enum SortOption {
        NEWEST, HELPFUL, RATING_HIGH, RATING_LOW
    }
}