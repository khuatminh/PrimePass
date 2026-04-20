package vn.devpro.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.devpro.marketplace.entity.Product;
import vn.devpro.marketplace.entity.ProductKey;
import vn.devpro.marketplace.entity.ProductVariant;

import java.util.List;
import java.util.Optional;

public interface ProductKeyRepository extends JpaRepository<ProductKey, Integer> {

    Optional<ProductKey> findFirstByVariantAndStatus(ProductVariant variant,
                                                      ProductKey.KeyStatus status);

    Optional<ProductKey> findFirstByProductAndVariantIsNullAndStatus(Product product,
                                                                      ProductKey.KeyStatus status);

    List<ProductKey> findByProductAndStatus(Product product, ProductKey.KeyStatus status);

    List<ProductKey> findByVariantAndStatus(ProductVariant variant, ProductKey.KeyStatus status);

    long countByProductAndStatus(Product product, ProductKey.KeyStatus status);
}
