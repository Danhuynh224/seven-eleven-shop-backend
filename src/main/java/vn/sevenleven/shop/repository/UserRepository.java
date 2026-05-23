package vn.sevenleven.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.sevenleven.shop.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
