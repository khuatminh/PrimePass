package vn.devpro.marketplace.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.devpro.marketplace.dto.Cart;
import vn.devpro.marketplace.entity.Order;
import vn.devpro.marketplace.security.UserPrincipal;
import vn.devpro.marketplace.service.CartService;
import vn.devpro.marketplace.service.OrderService;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController extends BaseController {

    private final OrderService orderService;
    private final CartService cartService;

    @PostMapping("/create")
    public String createOrder(@RequestParam(required = false) String couponCode,
                              HttpSession session,
                              Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Cart cart = cartService.getCart(session);
        try {
            Order order = orderService.createOrder(principal.getUser(), cart, couponCode);
            cartService.clear(session);
            return "redirect:/payment/vnpay?orderId=" + order.getId();
        } catch (IllegalArgumentException e) {
            session.setAttribute("cartError", e.getMessage());
            return "redirect:/cart";
        }
    }

    @GetMapping("/history")
    public String orderHistory(Model model, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        model.addAttribute("orders", orderService.findByUser(principal.getUser()));
        return "order-history";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Integer id, Model model, Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Order order = orderService.findById(id);
        if (!order.getUser().getId().equals(principal.getUser().getId())) return "redirect:/";
        model.addAttribute("order", order);
        return "order-detail";
    }
}
