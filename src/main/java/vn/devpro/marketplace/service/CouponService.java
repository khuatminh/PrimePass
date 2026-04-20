package vn.devpro.marketplace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.devpro.marketplace.entity.Coupon;
import vn.devpro.marketplace.exception.ResourceNotFoundException;
import vn.devpro.marketplace.repository.CouponRepository;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional(readOnly = true)
    public Coupon validate(String code, Long orderTotal) {
        Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(code)
            .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không hợp lệ hoặc đã hết hiệu lực"));
        LocalDate today = LocalDate.now();
        if (coupon.getStartDate() != null && today.isBefore(coupon.getStartDate()))
            throw new IllegalArgumentException("Mã giảm giá chưa có hiệu lực");
        if (coupon.getEndDate() != null && today.isAfter(coupon.getEndDate()))
            throw new IllegalArgumentException("Mã giảm giá đã hết hạn");
        if (coupon.getUsedCount() >= coupon.getMaxUses())
            throw new IllegalArgumentException("Mã giảm giá đã đạt giới hạn sử dụng");
        if (orderTotal < coupon.getMinOrderAmount())
            throw new IllegalArgumentException("Đơn hàng tối thiểu " + coupon.getMinOrderAmount() + "đ để dùng mã này");
        return coupon;
    }

    public Long calculateDiscount(Coupon coupon, Long orderTotal) {
        if (coupon.getDiscountType() == Coupon.DiscountType.percent) {
            return orderTotal * coupon.getDiscountValue() / 100;
        }
        return Math.min(coupon.getDiscountValue(), orderTotal);
    }

    @Transactional
    public void incrementUsage(Coupon coupon) {
        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);
    }

    public List<Coupon> findAll() { return couponRepository.findAll(); }

    @Transactional
    public Coupon save(Coupon coupon) { return couponRepository.save(coupon); }

    public Coupon findById(Integer id) {
        return couponRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
    }
}
