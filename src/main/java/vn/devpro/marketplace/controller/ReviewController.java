package vn.devpro.marketplace.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import vn.devpro.marketplace.entity.Product;
import vn.devpro.marketplace.security.UserPrincipal;
import vn.devpro.marketplace.service.ProductService;
import vn.devpro.marketplace.service.ReviewService;

@Controller
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController extends BaseController {

    private final ReviewService reviewService;
    private final ProductService productService;

    @PostMapping("/add")
    public String addReview(
            @RequestParam Integer productId,
            @RequestParam Integer rating,
            @RequestParam(required = false) String comment,
            Authentication authentication) {

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Product product = productService.findById(productId);
        reviewService.addReview(principal.getUser(), product, rating, comment);
        return "redirect:/product/" + product.getSlug() + "#reviews";
    }
}
