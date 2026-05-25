package vn.sevenleven.shop.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.sevenleven.shop.entity.Product;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = {"category"})
    @Query(value = "SELECT p FROM Product p WHERE p.deletedAt IS NULL " +
                   "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))) " +
                   "AND (:categoryId IS NULL OR p.category.id = :categoryId)",
           countQuery = "SELECT COUNT(p) FROM Product p WHERE p.deletedAt IS NULL " +
                        "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS String), '%'))) " +
                        "AND (:categoryId IS NULL OR p.category.id = :categoryId)")
    Page<Product> findActiveProducts(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Product> findActiveById(@Param("id") Long id);

    // Acquires a pessimistic write lock to prevent concurrent overselling
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Product> findByIdWithLock(@Param("id") Long id);
}
