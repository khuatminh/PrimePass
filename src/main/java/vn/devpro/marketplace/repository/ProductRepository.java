package vn.devpro.marketplace.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.devpro.marketplace.entity.Category;
import vn.devpro.marketplace.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer>,
        JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    Page<Product> findByCategoryAndIsActive(Category category, Boolean isActive, Pageable pageable);

    List<Product> findByIsFeaturedTrueAndIsActiveTrue();

    List<Product> findByCategoryAndIsActiveTrue(Category category);
}
