package vn.sevenleven.shop.mapper;

import org.mapstruct.Mapper;
import vn.sevenleven.shop.dto.response.CategoryResponse;
import vn.sevenleven.shop.entity.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);
}
