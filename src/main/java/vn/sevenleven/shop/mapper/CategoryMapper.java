package vn.sevenleven.shop.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import vn.sevenleven.shop.dto.request.UpdateCategoryRequest;
import vn.sevenleven.shop.dto.response.CategoryResponse;
import vn.sevenleven.shop.entity.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);

    void updateEntity(UpdateCategoryRequest request, @MappingTarget Category category);
}
