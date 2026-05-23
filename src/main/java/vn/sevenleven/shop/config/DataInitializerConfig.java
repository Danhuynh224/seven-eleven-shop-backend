package vn.sevenleven.shop.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.sevenleven.shop.entity.User;
import vn.sevenleven.shop.enums.Role;
import vn.sevenleven.shop.repository.UserRepository;

import java.util.List;

/**
 * Seeds initial users at startup if none exist.
 * BCrypt hashes are generated at runtime, avoiding hardcoded values in SQL migrations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializerConfig implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            seedUsers();
        }
    }

    private void seedUsers() {
        List<User> users = List.of(
                User.builder()
                        .username("admin")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .role(Role.ADMIN)
                        .fullName("Administrator")
                        .build(),
                User.builder()
                        .username("customer1")
                        .passwordHash(passwordEncoder.encode("customer123"))
                        .role(Role.CUSTOMER)
                        .fullName("Nguyễn Văn An")
                        .build(),
                User.builder()
                        .username("customer2")
                        .passwordHash(passwordEncoder.encode("user123"))
                        .role(Role.CUSTOMER)
                        .fullName("Trần Thị Bình")
                        .build()
        );

        userRepository.saveAll(users);
        log.info("Seeded {} initial users (admin, customer1, customer2)", users.size());
    }
}
