package vn.devpro.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer productId;
    private Integer variantId;
    private String productName;
    private String variantLabel;
    private String imageUrl;
    private Long price;
    private Integer quantity;

    public Long getSubtotal() {
        return price * quantity;
    }
}
