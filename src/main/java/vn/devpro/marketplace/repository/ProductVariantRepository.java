package vn.devpro.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.devpro.marketplace.entity.Product;
import vn.devpro.marketplace.entity.ProductVariant;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {

    List<ProductVariant> findByProductAndIsActiveTrue(Product product);

    List<ProductVariant> findByProduct(Product product);
}
