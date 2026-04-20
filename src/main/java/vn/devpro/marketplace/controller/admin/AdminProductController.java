package vn.devpro.marketplace.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.devpro.marketplace.entity.*;
import vn.devpro.marketplace.repository.ProductVariantRepository;
import vn.devpro.marketplace.repository.ProductVariantTypeRepository;
import vn.devpro.marketplace.service.CategoryService;
import vn.devpro.marketplace.service.ProductService;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Controller
@RequestMapping("/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ProductVariantRepository variantRepository;
    private final ProductVariantTypeRepository variantTypeRepository;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        Page<Product> products = productService.search(keyword, null, page, 10);
        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword);
        return "admin/product/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("deliveryTypes", Product.DeliveryType.values());
        return "admin/product/add";
    }

    @PostMapping("/add")
    public String saveProduct(@RequestParam String name,
                              @RequestParam String description,
                              @RequestParam Integer categoryId,
                              @RequestParam Long originalPrice,
                              @RequestParam Long salePrice,
                              @RequestParam String deliveryType,
                              @RequestParam(required = false) String warrantyInfo,
                              @RequestParam(required = false) String isFeatured,
                              @RequestParam(required = false) MultipartFile imageFile,
                              RedirectAttributes ra) throws IOException {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setCategory(categoryService.findById(categoryId));
        product.setOriginalPrice(originalPrice);
        product.setSalePrice(salePrice);
        product.setDeliveryType(Product.DeliveryType.valueOf(deliveryType));
        product.setWarrantyInfo(warrantyInfo);
        product.setIsFeatured("on".equals(isFeatured));
        product.setIsActive(true);
        product.setSlug(toSlug(name) + "-" + System.currentTimeMillis());

        if (imageFile != null && !imageFile.isEmpty()) {
            String filename = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            Path path = Paths.get(uploadDir);
            Files.createDirectories(path);
            imageFile.transferTo(path.resolve(filename));
            product.setImageUrl("/uploads/" + filename);
        }
        productService.save(product);
        ra.addFlashAttribute("successMessage", "Them san pham thanh cong");
        return "redirect:/admin/products";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        model.addAttribute("product", productService.findById(id));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("deliveryTypes", Product.DeliveryType.values());
        return "admin/product/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateProduct(@PathVariable Integer id,
                                @RequestParam String name,
                                @RequestParam String description,
                                @RequestParam Integer categoryId,
                                @RequestParam Long originalPrice,
                                @RequestParam Long salePrice,
                                @RequestParam String deliveryType,
                                @RequestParam(required = false) String warrantyInfo,
                                @RequestParam(required = false) String isFeatured,
                                @RequestParam(required = false) String isActive,
                                @RequestParam(required = false) MultipartFile imageFile,
                                RedirectAttributes ra) throws IOException {
        Product product = productService.findById(id);
        product.setName(name);
        product.setDescription(description);
        product.setCategory(categoryService.findById(categoryId));
        product.setOriginalPrice(originalPrice);
        product.setSalePrice(salePrice);
        product.setDeliveryType(Product.DeliveryType.valueOf(deliveryType));
        product.setWarrantyInfo(warrantyInfo);
        product.setIsFeatured("on".equals(isFeatured));
        product.setIsActive("on".equals(isActive));

        if (imageFile != null && !imageFile.isEmpty()) {
            String filename = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            Path path = Paths.get(uploadDir);
            Files.createDirectories(path);
            imageFile.transferTo(path.resolve(filename));
            product.setImageUrl("/uploads/" + filename);
        }
        productService.save(product);
        ra.addFlashAttribute("successMessage", "Cap nhat thanh cong");
        return "redirect:/admin/products";
    }

    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Integer id, RedirectAttributes ra) {
        productService.deleteById(id);
        ra.addFlashAttribute("successMessage", "Da xoa san pham");
        return "redirect:/admin/products";
    }

    @GetMapping("/{id}/variants")
    public String manageVariants(@PathVariable Integer id, Model model) {
        Product product = productService.findById(id);
        model.addAttribute("product", product);
        model.addAttribute("variants", variantRepository.findByProductAndIsActiveTrue(product));
        model.addAttribute("variantTypes", variantTypeRepository.findByProductOrderBySortOrder(product));
        return "admin/product/variants";
    }

    @PostMapping("/{id}/variants/add")
    public String addVariant(@PathVariable Integer id,
                             @RequestParam String variantLabel,
                             @RequestParam Long price,
                             @RequestParam Integer stockCount,
                             RedirectAttributes ra) {
        Product product = productService.findById(id);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantLabel(variantLabel);
        variant.setPrice(price);
        variant.setStockCount(stockCount);
        variant.setIsActive(true);
        variantRepository.save(variant);
        product.setStockCount(product.getStockCount() + stockCount);
        productService.save(product);
        ra.addFlashAttribute("successMessage", "Them variant thanh cong");
        return "redirect:/admin/products/" + id + "/variants";
    }

    @GetMapping("/{productId}/variants/delete/{variantId}")
    public String deleteVariant(@PathVariable Integer productId,
                                @PathVariable Integer variantId,
                                RedirectAttributes ra) {
        ProductVariant variant = variantRepository.findById(variantId).orElseThrow();
        Product product = productService.findById(productId);
        product.setStockCount(Math.max(0, product.getStockCount() - variant.getStockCount()));
        productService.save(product);
        variantRepository.deleteById(variantId);
        ra.addFlashAttribute("successMessage", "Da xoa variant");
        return "redirect:/admin/products/" + productId + "/variants";
    }

    private String toSlug(String name) {
        return name.toLowerCase()
            .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
            .replaceAll("[èéẹẻẽêềếệểễ]", "e")
            .replaceAll("[ìíịỉĩ]", "i")
            .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
            .replaceAll("[ùúụủũưừứựửữ]", "u")
            .replaceAll("[ỳýỵỷỹ]", "y")
            .replaceAll("[đ]", "d")
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-").trim();
    }
}
