package vn.sevenleven.shop.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sevenleven.shop.dto.request.CreateCategoryRequest;
import vn.sevenleven.shop.dto.request.UpdateCategoryRequest;
import vn.sevenleven.shop.dto.response.CategoryResponse;
import vn.sevenleven.shop.entity.Category;
import vn.sevenleven.shop.exception.BusinessException;
import vn.sevenleven.shop.exception.ResourceNotFoundException;
import vn.sevenleven.shop.mapper.CategoryMapper;
import vn.sevenleven.shop.repository.CategoryRepository;
import vn.sevenleven.shop.repository.ProductRepository;
import vn.sevenleven.shop.service.CategoryService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Cacheable(value = "categories", key = "'all'")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new BusinessException("Category name already exists: " + request.name());
        }
        Category saved = categoryRepository.save(
                Category.builder().name(request.name()).build());
        return categoryMapper.toResponse(saved);
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (!category.getName().equals(request.name())
                && categoryRepository.existsByName(request.name())) {
            throw new BusinessException("Category name already exists: " + request.name());
        }

        categoryMapper.updateEntity(request, category);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (productRepository.existsByCategory_IdAndDeletedAtIsNull(id)) {
            throw new BusinessException(
                    "Cannot delete category '" + category.getName() + "': it still has active products");
        }

        categoryRepository.delete(category);
    }
}
