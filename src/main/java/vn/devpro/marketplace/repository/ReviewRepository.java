package vn.devpro.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.devpro.marketplace.entity.Product;
import vn.devpro.marketplace.entity.Review;
import vn.devpro.marketplace.entity.User;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findByProductOrderByCreatedAtDesc(Product product);

    boolean existsByUserAndProduct(User user, Product product);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double avgRatingByProduct(@Param("productId") Integer productId);
}
