package vn.sevenleven.shop.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name
) {}
