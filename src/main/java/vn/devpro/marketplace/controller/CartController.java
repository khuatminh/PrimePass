package vn.devpro.marketplace.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.devpro.marketplace.dto.Cart;
import vn.devpro.marketplace.entity.Coupon;
import vn.devpro.marketplace.entity.Product;
import vn.devpro.marketplace.entity.ProductVariant;
import vn.devpro.marketplace.repository.ProductVariantRepository;
import vn.devpro.marketplace.service.CartService;
import vn.devpro.marketplace.service.CouponService;
import vn.devpro.marketplace.service.ProductService;

import java.util.Map;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController extends BaseController {

    private final CartService cartService;
    private final ProductService productService;
    private final ProductVariantRepository variantRepository;
    private final CouponService couponService;

    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        model.addAttribute("cart", cartService.getCart(session));
        String cartError = (String) session.getAttribute("cartError");
        if (cartError != null) {
            model.addAttribute("cartError", cartError);
            session.removeAttribute("cartError");
        }
        return "cart";
    }

    @PostMapping("/add")
    public String addToCart(@RequestParam Integer productId,
                            @RequestParam(required = false) Integer variantId,
                            @RequestParam(defaultValue = "1") int quantity,
                            HttpSession session) {
        Product product = productService.findById(productId);
        ProductVariant variant = variantId != null
            ? variantRepository.findById(variantId).orElse(null)
            : null;
        cartService.addItem(session, product, variant, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String removeFromCart(@RequestParam Integer productId,
                                 @RequestParam(required = false) Integer variantId,
                                 HttpSession session) {
        cartService.removeItem(session, productId, variantId);
        return "redirect:/cart";
    }

    @GetMapping("/validate-coupon")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateCoupon(@RequestParam String code,
                                                               HttpSession session) {
        Cart cart = cartService.getCart(session);
        try {
            Coupon coupon = couponService.validate(code, cart.getTotalPrice());
            long discount = couponService.calculateDiscount(coupon, cart.getTotalPrice());
            return ResponseEntity.ok(Map.of(
                "valid", true,
                "discount", discount,
                "finalAmount", cart.getTotalPrice() - discount,
                "message", "Áp dụng thành công! Giảm ₫" + String.format("%,.0f", (double) discount)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Map.of("valid", false, "message", e.getMessage()));
        }
    }
}
