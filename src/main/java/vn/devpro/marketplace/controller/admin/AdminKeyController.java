package vn.devpro.marketplace.controller.admin;

import lombok.RequiredArgsConstructor;
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

    @GetMapping
    public String list(@RequestParam(required = false) Integer productId, Model model) {
        var keys = productId != null
            ? productKeyRepository.findByProductAndStatus(
                productRepository.findById(productId).orElseThrow(), ProductKey.KeyStatus.available)
            : productKeyRepository.findAll();
        model.addAttribute("keys", keys);
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("selectedProductId", productId);
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
        ra.addFlashAttribute("successMessage", "Key da duoc them vao kho");
        return "redirect:/admin/keys?productId=" + productId;
    }
}
