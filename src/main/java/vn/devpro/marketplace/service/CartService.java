package vn.devpro.marketplace.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.devpro.marketplace.dto.Cart;
import vn.devpro.marketplace.dto.CartItem;
import vn.devpro.marketplace.entity.Product;
import vn.devpro.marketplace.entity.ProductVariant;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CartService {

    private static final String CART_KEY = "cart";

    public Cart getCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute(CART_KEY);
        if (cart == null) {
            cart = new Cart();
            session.setAttribute(CART_KEY, cart);
        }
        return cart;
    }

    public void addItem(HttpSession session, Product product, ProductVariant variant, int quantity) {
        Cart cart = getCart(session);
        Integer variantId = variant != null ? variant.getId() : null;
        CartItem existing = cart.findItem(product.getId(), variantId);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            CartItem item = new CartItem();
            item.setProductId(product.getId());
            item.setVariantId(variantId);
            item.setProductName(product.getName());
            item.setVariantLabel(variant != null ? variant.getVariantLabel() : null);
            item.setImageUrl(product.getImageUrl());
            item.setPrice(variant != null ? variant.getPrice() : product.getSalePrice());
            item.setQuantity(quantity);
            cart.getItems().add(item);
        }
        session.setAttribute(CART_KEY, cart);
    }

    public void removeItem(HttpSession session, Integer productId, Integer variantId) {
        Cart cart = getCart(session);
        cart.getItems().removeIf(i ->
            i.getProductId().equals(productId) && Objects.equals(i.getVariantId(), variantId));
        session.setAttribute(CART_KEY, cart);
    }

    public void clear(HttpSession session) {
        session.removeAttribute(CART_KEY);
    }
}
