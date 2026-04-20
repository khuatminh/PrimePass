package vn.devpro.marketplace.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.devpro.marketplace.entity.Category;
import vn.devpro.marketplace.repository.ProductRepository;
import vn.devpro.marketplace.service.CategoryService;

@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;
    private final ProductRepository productRepository;

    @GetMapping
    public String list(Model model) {
        var all = categoryService.findAll();
        model.addAttribute("categories", all);
        model.addAttribute("totalCategories", all.size());
        model.addAttribute("activeCategories", all.stream().filter(Category::getIsActive).count());
        model.addAttribute("totalProducts", productRepository.count());
        model.addAttribute("newCategory", new Category());
        return "admin/category/list";
    }

    @PostMapping("/add")
    public String addCategory(@ModelAttribute("newCategory") Category category, RedirectAttributes ra) {
        category.setSlug(toSlug(category.getName()));
        categoryService.save(category);
        ra.addFlashAttribute("successMessage", "Them danh muc thanh cong");
        return "redirect:/admin/categories";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        model.addAttribute("category", categoryService.findById(id));
        return "admin/category/edit";
    }

    @PostMapping("/edit/{id}")
    public String editCategory(@PathVariable Integer id,
                               @ModelAttribute Category form,
                               RedirectAttributes ra) {
        Category existing = categoryService.findById(id);
        existing.setName(form.getName());
        existing.setDescription(form.getDescription());
        existing.setIconUrl(form.getIconUrl());
        existing.setIsActive(form.getIsActive());
        existing.setSortOrder(form.getSortOrder());
        categoryService.save(existing);
        ra.addFlashAttribute("successMessage", "Cap nhat thanh cong");
        return "redirect:/admin/categories";
    }

    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Integer id, RedirectAttributes ra) {
        categoryService.deleteById(id);
        ra.addFlashAttribute("successMessage", "Da xoa danh muc");
        return "redirect:/admin/categories";
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
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-").trim();
    }
}
