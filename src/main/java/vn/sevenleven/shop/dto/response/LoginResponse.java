package vn.sevenleven.shop.dto.response;

public record LoginResponse(
        String accessToken,
        String tokenType,
        Long expiresIn,
        UserResponse user
) {}
