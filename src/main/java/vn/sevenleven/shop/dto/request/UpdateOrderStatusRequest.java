package vn.sevenleven.shop.dto.request;

import jakarta.validation.constraints.NotNull;
import vn.sevenleven.shop.enums.OrderStatus;

public record UpdateOrderStatusRequest(
        @NotNull(message = "Status is required")
        OrderStatus status
) {}
