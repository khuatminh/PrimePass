package vn.devpro.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.devpro.marketplace.entity.Coupon;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Integer> {

    Optional<Coupon> findByCodeAndIsActiveTrue(String code);

    Optional<Coupon> findByCode(String code);
}
