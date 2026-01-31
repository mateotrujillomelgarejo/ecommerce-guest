package pe.takiq.ecommerce.pricing_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.takiq.ecommerce.pricing_service.model.Coupon;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCodeAndActiveTrue(String code);

    @Modifying
    @Query("UPDATE Coupon c SET c.usesCount = c.usesCount + 1 WHERE c.code = :code")
    int incrementUses(@Param("code") String code);
}