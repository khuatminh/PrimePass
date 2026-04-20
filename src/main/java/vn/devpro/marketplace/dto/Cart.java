package vn.devpro.marketplace.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Cart implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<CartItem> items = new ArrayList<>();

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public long getTotalPrice() {
        return items.stream().mapToLong(CartItem::getSubtotal).sum();
    }

    public int getTotalQuantity() {
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public CartItem findItem(Integer productId, Integer variantId) {
        return items.stream()
                .filter(i -> i.getProductId().equals(productId)
                        && Objects.equals(i.getVariantId(), variantId))
                .findFirst()
                .orElse(null);
    }
}
