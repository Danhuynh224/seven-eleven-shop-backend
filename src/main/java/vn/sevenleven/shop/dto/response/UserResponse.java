package vn.sevenleven.shop.dto.response;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        String role
) {}
