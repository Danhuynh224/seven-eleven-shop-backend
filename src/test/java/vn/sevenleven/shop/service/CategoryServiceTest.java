package vn.sevenleven.shop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.sevenleven.shop.dto.request.CreateCategoryRequest;
import vn.sevenleven.shop.dto.request.UpdateCategoryRequest;
import vn.sevenleven.shop.dto.response.CategoryResponse;
import vn.sevenleven.shop.entity.Category;
import vn.sevenleven.shop.exception.BusinessException;
import vn.sevenleven.shop.exception.ResourceNotFoundException;
import vn.sevenleven.shop.mapper.CategoryMapper;
import vn.sevenleven.shop.repository.CategoryRepository;
import vn.sevenleven.shop.repository.ProductRepository;
import vn.sevenleven.shop.service.impl.CategoryServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category drinkCategory;
    private CategoryResponse drinkResponse;

    @BeforeEach
    void setUp() {
        drinkCategory = Category.builder().id(1L).name("Drinks").build();
        drinkResponse = new CategoryResponse(1L, "Drinks", LocalDateTime.now());
    }

    // ──────────────────────────────────────────────────────────
    // getAllCategories
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllCategories: returns all categories mapped to response")
    void getAllCategories_shouldReturnMappedList() {
        Category snack = Category.builder().id(2L).name("Snacks").build();
        CategoryResponse snackResponse = new CategoryResponse(2L, "Snacks", LocalDateTime.now());

        when(categoryRepository.findAll()).thenReturn(List.of(drinkCategory, snack));
        when(categoryMapper.toResponse(drinkCategory)).thenReturn(drinkResponse);
        when(categoryMapper.toResponse(snack)).thenReturn(snackResponse);

        List<CategoryResponse> result = categoryService.getAllCategories();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CategoryResponse::name)
                .containsExactly("Drinks", "Snacks");
    }

    @Test
    @DisplayName("getAllCategories: empty table — returns empty list")
    void getAllCategories_whenNone_shouldReturnEmptyList() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        assertThat(categoryService.getAllCategories()).isEmpty();
    }

    // ──────────────────────────────────────────────────────────
    // createCategory
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createCategory: new name — saves and returns response")
    void createCategory_newName_shouldSaveAndReturn() {
        CreateCategoryRequest request = new CreateCategoryRequest("Drinks");

        when(categoryRepository.existsByName("Drinks")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(drinkCategory);
        when(categoryMapper.toResponse(drinkCategory)).thenReturn(drinkResponse);

        CategoryResponse result = categoryService.createCategory(request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Drinks");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("createCategory: duplicate name — throws BusinessException, nothing saved")
    void createCategory_duplicateName_shouldThrowBusinessException() {
        CreateCategoryRequest request = new CreateCategoryRequest("Drinks");

        when(categoryRepository.existsByName("Drinks")).thenReturn(true);

        assertThrows(BusinessException.class, () -> categoryService.createCategory(request));

        verify(categoryRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────
    // updateCategory
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateCategory: new unique name — updates and returns response")
    void updateCategory_newUniqueName_shouldUpdateAndReturn() {
        UpdateCategoryRequest request = new UpdateCategoryRequest("Beverages");
        CategoryResponse updatedResponse = new CategoryResponse(1L, "Beverages", LocalDateTime.now());

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(drinkCategory));
        when(categoryRepository.existsByName("Beverages")).thenReturn(false);
        when(categoryRepository.save(drinkCategory)).thenReturn(drinkCategory);
        when(categoryMapper.toResponse(drinkCategory)).thenReturn(updatedResponse);

        CategoryResponse result = categoryService.updateCategory(1L, request);

        assertThat(result.name()).isEqualTo("Beverages");
        verify(categoryMapper).updateEntity(request, drinkCategory);
        verify(categoryRepository).save(drinkCategory);
    }

    @Test
    @DisplayName("updateCategory: same name unchanged — no duplicate check, saves successfully")
    void updateCategory_sameName_shouldSaveWithoutDuplicateCheck() {
        UpdateCategoryRequest request = new UpdateCategoryRequest("Drinks");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(drinkCategory));
        when(categoryRepository.save(drinkCategory)).thenReturn(drinkCategory);
        when(categoryMapper.toResponse(drinkCategory)).thenReturn(drinkResponse);

        CategoryResponse result = categoryService.updateCategory(1L, request);

        assertThat(result.name()).isEqualTo("Drinks");
        verify(categoryRepository, never()).existsByName(anyString());
    }

    @Test
    @DisplayName("updateCategory: name taken by another category — throws BusinessException")
    void updateCategory_nameTakenByOther_shouldThrowBusinessException() {
        UpdateCategoryRequest request = new UpdateCategoryRequest("Snacks");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(drinkCategory));
        when(categoryRepository.existsByName("Snacks")).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> categoryService.updateCategory(1L, request));

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateCategory: category not found — throws ResourceNotFoundException")
    void updateCategory_notFound_shouldThrowResourceNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.updateCategory(99L, new UpdateCategoryRequest("X")));

        verify(categoryRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────
    // deleteCategory
    // ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteCategory: no active products — deletes successfully")
    void deleteCategory_noActiveProducts_shouldDelete() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(drinkCategory));
        when(productRepository.existsByCategory_IdAndDeletedAtIsNull(1L)).thenReturn(false);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).delete(drinkCategory);
    }

    @Test
    @DisplayName("deleteCategory: has active products — throws BusinessException, not deleted")
    void deleteCategory_hasActiveProducts_shouldThrowBusinessException() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(drinkCategory));
        when(productRepository.existsByCategory_IdAndDeletedAtIsNull(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> categoryService.deleteCategory(1L));

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteCategory: category not found — throws ResourceNotFoundException")
    void deleteCategory_notFound_shouldThrowResourceNotFoundException() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.deleteCategory(99L));

        verify(categoryRepository, never()).delete(any());
        verify(productRepository, never()).existsByCategory_IdAndDeletedAtIsNull(anyLong());
    }
}
