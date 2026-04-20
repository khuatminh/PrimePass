package vn.devpro.marketplace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.devpro.marketplace.dto.Cart;
import vn.devpro.marketplace.dto.CartItem;
import vn.devpro.marketplace.entity.*;
import vn.devpro.marketplace.exception.ResourceNotFoundException;
import vn.devpro.marketplace.repository.*;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CouponService couponService;

    public Order createOrder(User user, Cart cart, String couponCode) {
        if (cart.getItems().isEmpty()) throw new IllegalStateException("Giỏ hàng trống");

        long totalAmount = cart.getTotalPrice();
        long discountAmount = 0L;
        Coupon coupon = null;

        if (couponCode != null && !couponCode.isBlank()) {
            coupon = couponService.validate(couponCode, totalAmount);
            discountAmount = couponService.calculateDiscount(coupon, totalAmount);
        }

        Order order = new Order();
        order.setUser(user);
        order.setCoupon(coupon);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(totalAmount - discountAmount);
        order.setStatus(Order.OrderStatus.pending);
        order.setVnpayTxnRef("TXN" + System.currentTimeMillis());
        Order saved = orderRepository.save(order);

        for (CartItem item : cart.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setOrder(saved);
            oi.setProduct(productRepository.findById(item.getProductId()).orElseThrow());
            if (item.getVariantId() != null)
                oi.setVariant(variantRepository.findById(item.getVariantId()).orElse(null));
            oi.setQuantity(item.getQuantity());
            oi.setUnitPrice(item.getPrice());
            oi.setSubtotal(item.getSubtotal());
            orderItemRepository.save(oi);
        }

        if (coupon != null) couponService.incrementUsage(coupon);
        return saved;
    }

    @Transactional(readOnly = true)
    public Order findById(Integer id) {
        return orderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
    }

    @Transactional(readOnly = true)
    public List<Order> findByUser(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public Order findByTxnRef(String txnRef) {
        return orderRepository.findByVnpayTxnRef(txnRef)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "txnRef", txnRef));
    }

    public void markPaid(Order order, String vnpayTransactionId) {
        order.setStatus(Order.OrderStatus.paid);
        order.setVnpayTransactionId(vnpayTransactionId);
        order.setPaymentMethod("VNPay");
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    public void markCompleted(Order order) {
        order.setStatus(Order.OrderStatus.completed);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    public void markCancelled(Order order) {
        order.setStatus(Order.OrderStatus.cancelled);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() { return orderRepository.findAll(); }
}
