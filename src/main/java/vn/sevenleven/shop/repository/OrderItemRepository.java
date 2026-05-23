package vn.sevenleven.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.sevenleven.shop.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
