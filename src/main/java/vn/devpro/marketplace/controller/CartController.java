package vn.devpro.marketplace.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.devpro.marketplace.entity.Product;
import vn.devpro.marketplace.entity.ProductVariant;
import vn.devpro.marketplace.repository.ProductVariantRepository;
import vn.devpro.marketplace.service.CartService;
import vn.devpro.marketplace.service.ProductService;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController extends BaseController {

    private final CartService cartService;
    private final ProductService productService;
    private final ProductVariantRepository variantRepository;

    @GetMapping
    public String viewCart(HttpSession session, Model model) {
        model.addAttribute("cart", cartService.getCart(session));
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
}
