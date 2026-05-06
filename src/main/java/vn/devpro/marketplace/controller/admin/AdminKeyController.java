package vn.devpro.marketplace.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.devpro.marketplace.entity.*;
import vn.devpro.marketplace.repository.*;

@Controller
@RequestMapping("/admin/keys")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminKeyController {

    private final ProductKeyRepository productKeyRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;

    private static final int PAGE_SIZE = 25;

    @GetMapping
    public String list(@RequestParam(required = false) Integer productId,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        PageRequest pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("id").descending());
        Page<ProductKey> keys = productId != null
            ? productKeyRepository.findByProduct(
                productRepository.findById(productId).orElseThrow(), pageable)
            : productKeyRepository.findAll(pageable);
        model.addAttribute("keys", keys);
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("variants", variantRepository.findAll());
        model.addAttribute("selectedProductId", productId);
        model.addAttribute("currentPage", page);
        return "admin/key/list";
    }

    @PostMapping("/add")
    public String addKey(@RequestParam Integer productId,
                         @RequestParam(required = false) Integer variantId,
                         @RequestParam String keyType,
                         @RequestParam(required = false) String serialKey,
                         @RequestParam(required = false) String accountEmail,
                         @RequestParam(required = false) String accountPassword,
                         RedirectAttributes ra) {
        ProductKey key = new ProductKey();
        key.setProduct(productRepository.findById(productId).orElseThrow());
        if (variantId != null) key.setVariant(variantRepository.findById(variantId).orElse(null));
        key.setKeyType(ProductKey.KeyType.valueOf(keyType));
        key.setSerialKey(serialKey);
        key.setAccountEmail(accountEmail);
        key.setAccountPassword(accountPassword);
        key.setStatus(ProductKey.KeyStatus.available);
        productKeyRepository.save(key);
        ra.addFlashAttribute("successMessage", "Key đã được thêm vào kho");
        return "redirect:/admin/keys?productId=" + productId;
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        ProductKey key = productKeyRepository.findById(id).orElseThrow();
        model.addAttribute("key", key);
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("variants", variantRepository.findAll());
        model.addAttribute("statuses", ProductKey.KeyStatus.values());
        return "admin/key/edit";
    }

    @PostMapping("/edit/{id}")
    public String editKey(@PathVariable Integer id,
                          @RequestParam Integer productId,
                          @RequestParam(required = false) Integer variantId,
                          @RequestParam String keyType,
                          @RequestParam(required = false) String serialKey,
                          @RequestParam(required = false) String accountEmail,
                          @RequestParam(required = false) String accountPassword,
                          @RequestParam String status,
                          RedirectAttributes ra) {
        ProductKey key = productKeyRepository.findById(id).orElseThrow();
        key.setProduct(productRepository.findById(productId).orElseThrow());
        key.setVariant(variantId != null
            ? variantRepository.findById(variantId).orElse(null)
            : null);
        key.setKeyType(ProductKey.KeyType.valueOf(keyType));
        key.setSerialKey(serialKey);
        key.setAccountEmail(accountEmail);
        key.setAccountPassword(accountPassword);
        key.setStatus(ProductKey.KeyStatus.valueOf(status));
        productKeyRepository.save(key);
        ra.addFlashAttribute("successMessage", "Đã cập nhật key #" + id);
        return "redirect:/admin/keys";
    }

    @PostMapping("/delete/{id}")
    public String deleteKey(@PathVariable Integer id, RedirectAttributes ra) {
        ProductKey key = productKeyRepository.findById(id).orElseThrow();
        if (key.getStatus() == ProductKey.KeyStatus.sold) {
            ra.addFlashAttribute("errorMessage",
                "Không thể xóa key đã giao. Hãy đổi status sang 'available' hoặc cập nhật nội dung trước.");
            return "redirect:/admin/keys";
        }
        productKeyRepository.delete(key);
        ra.addFlashAttribute("successMessage", "Đã xóa key #" + id);
        return "redirect:/admin/keys";
    }
}
