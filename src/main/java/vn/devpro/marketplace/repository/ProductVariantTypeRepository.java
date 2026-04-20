package vn.devpro.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.devpro.marketplace.entity.Product;
import vn.devpro.marketplace.entity.ProductVariantType;

import java.util.List;

public interface ProductVariantTypeRepository extends JpaRepository<ProductVariantType, Integer> {

    List<ProductVariantType> findByProductOrderBySortOrder(Product product);
}
