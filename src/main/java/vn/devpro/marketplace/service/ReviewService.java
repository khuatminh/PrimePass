package vn.devpro.marketplace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.devpro.marketplace.entity.*;
import vn.devpro.marketplace.repository.OrderItemRepository;
import vn.devpro.marketplace.repository.ReviewRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional(readOnly = true)
    public List<Review> findByProduct(Product product) {
        return reviewRepository.findByProductOrderByCreatedAtDesc(product);
    }

    @Transactional(readOnly = true)
    public Double getAverageRating(Integer productId) {
        return reviewRepository.avgRatingByProduct(productId);
    }

    @Transactional(readOnly = true)
    public boolean canReview(User user, Product product) {
        return orderItemRepository.existsByOrderUserAndProductAndOrderStatus(
                user, product, Order.OrderStatus.completed);
    }

    public Review addReview(User user, Product product, Integer rating, String comment) {
        if (reviewRepository.existsByUserAndProduct(user, product)) {
            throw new IllegalStateException("You have already reviewed this product.");
        }
        Review review = new Review();
        review.setUser(user);
        review.setProduct(product);
        review.setRating(rating);
        review.setComment(comment);
        return reviewRepository.save(review);
    }
}
