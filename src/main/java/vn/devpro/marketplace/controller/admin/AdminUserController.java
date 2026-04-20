package vn.devpro.marketplace.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.devpro.marketplace.entity.User;
import vn.devpro.marketplace.repository.UserRepository;

@Controller
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "admin/user/list";
    }

    @PostMapping("/{id}/toggle-role")
    public String toggleRole(@PathVariable Integer id, RedirectAttributes ra) {
        User user = userRepository.findById(id).orElseThrow();
        user.setRole(user.getRole() == User.UserRole.admin ? User.UserRole.customer : User.UserRole.admin);
        userRepository.save(user);
        ra.addFlashAttribute("successMessage", "Doi role thanh cong");
        return "redirect:/admin/users";
    }
}
