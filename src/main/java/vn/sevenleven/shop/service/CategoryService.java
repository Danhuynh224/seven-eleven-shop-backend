package vn.sevenleven.shop.service;

import vn.sevenleven.shop.dto.request.CreateCategoryRequest;
import vn.sevenleven.shop.dto.request.UpdateCategoryRequest;
import vn.sevenleven.shop.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories();

    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(Long id, UpdateCategoryRequest request);

    void deleteCategory(Long id);
}
