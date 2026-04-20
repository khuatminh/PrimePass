package vn.devpro.marketplace.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.devpro.marketplace.dto.Cart;

public abstract class BaseController {

    protected Cart getCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute("cart");
        if (cart == null) {
            cart = new Cart();
            session.setAttribute("cart", cart);
        }
        return cart;
    }

    @ModelAttribute("cartItemCount")
    public int cartItemCount(HttpSession session) {
        return getCart(session).getTotalQuantity();
    }
}
