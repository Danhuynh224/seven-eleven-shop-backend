package vn.sevenleven.shop.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderCreatedEvent(
        Long orderId,
        Long userId,
        String username,
        BigDecimal totalAmount,
        int itemCount,
        LocalDateTime createdAt
) {}
