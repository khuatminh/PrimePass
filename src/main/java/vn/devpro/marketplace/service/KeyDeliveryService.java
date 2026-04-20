package vn.devpro.marketplace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.devpro.marketplace.entity.*;
import vn.devpro.marketplace.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KeyDeliveryService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductKeyRepository productKeyRepository;
    private final ProductVariantRepository variantRepository;

    @Transactional
    public void deliver(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrder(order);
        for (OrderItem item : items) {
            for (int i = 0; i < item.getQuantity(); i++) {
                Optional<ProductKey> keyOpt;
                if (item.getVariant() != null) {
                    keyOpt = productKeyRepository.findFirstByVariantAndStatus(
                        item.getVariant(), ProductKey.KeyStatus.available);
                } else {
                    keyOpt = productKeyRepository.findFirstByProductAndVariantIsNullAndStatus(
                        item.getProduct(), ProductKey.KeyStatus.available);
                }
                keyOpt.ifPresent(key -> {
                    key.setStatus(ProductKey.KeyStatus.sold);
                    key.setSoldAt(LocalDateTime.now());
                    key.setOrderItemId(item.getId());
                    productKeyRepository.save(key);

                    item.setProductKey(key);
                    orderItemRepository.save(item);
                });
            }
        }
        order.setStatus(Order.OrderStatus.completed);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }
}
