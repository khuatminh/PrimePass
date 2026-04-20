package vn.devpro.marketplace.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.devpro.marketplace.entity.Order;
import vn.devpro.marketplace.repository.OrderItemRepository;
import vn.devpro.marketplace.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    @GetMapping
    public String list(@RequestParam(required = false) String status, Model model) {
        List<Order> orders = orderRepository.findAll(Sort.by("createdAt").descending());
        if (status != null && !status.isBlank()) {
            Order.OrderStatus filterStatus = Order.OrderStatus.valueOf(status);
            orders = orders.stream().filter(o -> o.getStatus() == filterStatus).collect(Collectors.toList());
        }
        model.addAttribute("orders", orders);
        model.addAttribute("statuses", Order.OrderStatus.values());
        model.addAttribute("selectedStatus", status);
        return "admin/order/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Integer id, Model model) {
        Order order = orderRepository.findById(id).orElseThrow();
        model.addAttribute("order", order);
        model.addAttribute("items", orderItemRepository.findByOrder(order));
        model.addAttribute("statuses", Order.OrderStatus.values());
        return "admin/order/detail";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Integer id,
                               @RequestParam String status,
                               RedirectAttributes ra) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus(Order.OrderStatus.valueOf(status));
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        ra.addFlashAttribute("successMessage", "Cap nhat trang thai thanh cong");
        return "redirect:/admin/orders/" + id;
    }
}
