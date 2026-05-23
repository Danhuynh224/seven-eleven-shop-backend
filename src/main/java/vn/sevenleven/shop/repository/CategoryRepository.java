package vn.sevenleven.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.sevenleven.shop.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByName(String name);
}
