package vn.devpro.marketplace.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.devpro.marketplace.entity.Coupon;
import vn.devpro.marketplace.service.CouponService;

@Controller
@RequestMapping("/admin/coupons")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService couponService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("coupons", couponService.findAll());
        model.addAttribute("newCoupon", new Coupon());
        model.addAttribute("discountTypes", Coupon.DiscountType.values());
        return "admin/coupon/list";
    }

    @PostMapping("/add")
    public String addCoupon(@ModelAttribute("newCoupon") Coupon coupon, RedirectAttributes ra) {
        couponService.save(coupon);
        ra.addFlashAttribute("successMessage", "Them coupon thanh cong");
        return "redirect:/admin/coupons";
    }

    @PostMapping("/{id}/toggle")
    public String toggleActive(@PathVariable Integer id, RedirectAttributes ra) {
        Coupon coupon = couponService.findById(id);
        coupon.setIsActive(!coupon.getIsActive());
        couponService.save(coupon);
        ra.addFlashAttribute("successMessage", "Da cap nhat trang thai coupon");
        return "redirect:/admin/coupons";
    }
}
