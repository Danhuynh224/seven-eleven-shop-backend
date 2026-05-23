package vn.sevenleven.shop.service;

import vn.sevenleven.shop.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories();
}
