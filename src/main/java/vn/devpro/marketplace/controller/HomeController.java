package vn.devpro.marketplace.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.devpro.marketplace.entity.*;
import vn.devpro.marketplace.repository.ProductVariantRepository;
import vn.devpro.marketplace.repository.ProductVariantTypeRepository;
import vn.devpro.marketplace.security.UserPrincipal;
import vn.devpro.marketplace.service.CategoryService;
import vn.devpro.marketplace.service.ProductService;
import vn.devpro.marketplace.service.ReviewService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController extends BaseController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ProductVariantTypeRepository variantTypeRepository;
    private final ProductVariantRepository variantRepository;
    private final ReviewService reviewService;

    @ModelAttribute("categories")
    public List<Category> categories() {
        return categoryService.findAllActive();
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featuredProducts", productService.findFeatured());
        return "index";
    }

    @GetMapping("/marketplace")
    public String marketplace(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            Model model) {

        Page<Product> products = productService.search(keyword, categoryId, page, size);
        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());
        model.addAttribute("totalElements", products.getTotalElements());
        return "marketplace";
    }

    @GetMapping("/product/{slug}")
    public String productDetail(
            @PathVariable String slug,
            Model model,
            Authentication authentication) {

        Product product = productService.findBySlug(slug);
        List<ProductVariantType> variantTypes =
                variantTypeRepository.findByProductOrderBySortOrder(product);
        List<ProductVariant> variants =
                variantRepository.findByProductAndIsActiveTrue(product);
        List<Review> reviews = reviewService.findByProduct(product);
        Double avgRating = reviewService.getAverageRating(product.getId());

        model.addAttribute("product", product);
        model.addAttribute("variantTypes", variantTypes);
        model.addAttribute("variants", variants);
        model.addAttribute("reviews", reviews);
        model.addAttribute("avgRating", avgRating != null ? avgRating : 0.0);

        if (authentication != null && authentication.isAuthenticated()
                && !(authentication.getPrincipal() instanceof String)) {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            model.addAttribute("canReview",
                    reviewService.canReview(principal.getUser(), product));
        } else {
            model.addAttribute("canReview", false);
        }

        return "product-detail";
    }
}
