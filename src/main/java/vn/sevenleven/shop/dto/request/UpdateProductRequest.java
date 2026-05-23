package vn.sevenleven.shop.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 200, message = "Name must not exceed 200 characters")
        String name,

        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", message = "Price must be greater than 0")
        BigDecimal price,

        @NotNull(message = "Stock is required")
        @Min(value = 0, message = "Stock must be non-negative")
        Integer stock,

        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        String imageUrl,

        Long categoryId
) {}
