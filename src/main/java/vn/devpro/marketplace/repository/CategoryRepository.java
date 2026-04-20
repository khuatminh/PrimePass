package vn.devpro.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.devpro.marketplace.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer>,
        JpaSpecificationExecutor<Category> {

    Optional<Category> findBySlug(String slug);

    List<Category> findByIsActiveTrueOrderBySortOrderAsc();
}
