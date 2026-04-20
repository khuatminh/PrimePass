package vn.devpro.marketplace.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.devpro.marketplace.entity.Order;
import vn.devpro.marketplace.repository.OrderRepository;
import vn.devpro.marketplace.repository.ProductRepository;
import vn.devpro.marketplace.repository.UserRepository;

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @GetMapping({"", "/"})
    public String dashboard(Model model) {
        long totalOrders = orderRepository.count();
        long totalRevenue = orderRepository.findAll().stream()
            .filter(o -> o.getStatus() == Order.OrderStatus.completed
                      || o.getStatus() == Order.OrderStatus.paid)
            .mapToLong(Order::getFinalAmount).sum();
        long totalProducts = productRepository.count();
        long totalUsers    = userRepository.count();

        List<Order> recentOrders = orderRepository.findAll(
            PageRequest.of(0, 10, Sort.by("createdAt").descending())).getContent();

        model.addAttribute("totalOrders",   totalOrders);
        model.addAttribute("totalRevenue",  totalRevenue);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("totalUsers",    totalUsers);
        model.addAttribute("recentOrders",  recentOrders);
        return "admin/dashboard";
    }
}
