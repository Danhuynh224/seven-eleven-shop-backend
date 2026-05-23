package vn.sevenleven.shop.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import vn.sevenleven.shop.dto.request.CreateProductRequest;
import vn.sevenleven.shop.dto.response.ProductResponse;
import vn.sevenleven.shop.entity.Product;
import vn.sevenleven.shop.exception.ResourceNotFoundException;
import vn.sevenleven.shop.mapper.ProductMapper;
import vn.sevenleven.shop.repository.CategoryRepository;
import vn.sevenleven.shop.repository.ProductRepository;
import vn.sevenleven.shop.service.impl.ProductServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    @DisplayName("getProduct: found — returns mapped response")
    void getProduct_whenFound_shouldReturnProductResponse() {
        // Given
        Product product = Product.builder()
                .id(1L).name("Test Product")
                .price(new BigDecimal("35000")).stock(100)
                .build();

        ProductResponse expected = new ProductResponse(
                1L, "Test Product", null,
                new BigDecimal("35000"), 100, null,
                null, null, LocalDateTime.now(), LocalDateTime.now());

        when(productRepository.findActiveById(1L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(expected);

        // When
        ProductResponse result = productService.getProduct(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Test Product");
    }

    @Test
    @DisplayName("getProduct: not found — throws ResourceNotFoundException")
    void getProduct_whenNotFound_shouldThrowResourceNotFoundException() {
        when(productRepository.findActiveById(anyLong())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> productService.getProduct(999L));
    }

    @Test
    @DisplayName("createProduct: success — saved and response returned")
    void createProduct_success_shouldSaveAndReturnResponse() {
        // Given
        CreateProductRequest request = new CreateProductRequest(
                "New Product", "desc", new BigDecimal("25000"), 50, null, null);

        Product toSave = Product.builder()
                .name("New Product").price(new BigDecimal("25000")).stock(50).build();
        Product saved = Product.builder()
                .id(2L).name("New Product")
                .price(new BigDecimal("25000")).stock(50).build();

        ProductResponse expected = new ProductResponse(
                2L, "New Product", "desc",
                new BigDecimal("25000"), 50, null,
                null, null, LocalDateTime.now(), LocalDateTime.now());

        when(productMapper.toEntity(request)).thenReturn(toSave);
        when(productRepository.save(toSave)).thenReturn(saved);
        when(productMapper.toResponse(saved)).thenReturn(expected);

        // When
        ProductResponse result = productService.createProduct(request);

        // Then
        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.name()).isEqualTo("New Product");
        verify(productRepository).save(toSave);
    }

    @Test
    @DisplayName("getProducts: returns paged response mapped from repository")
    void getProducts_shouldReturnPageResponse() {
        // Given
        Product product = Product.builder()
                .id(1L).name("Product").price(BigDecimal.TEN).stock(5).build();

        ProductResponse response = new ProductResponse(
                1L, "Product", null, BigDecimal.TEN, 5, null,
                null, null, LocalDateTime.now(), LocalDateTime.now());

        Page<Product> productPage = new PageImpl<>(List.of(product));
        when(productRepository.findActiveProducts(any(), any(), any(Pageable.class)))
                .thenReturn(productPage);
        when(productMapper.toResponse(product)).thenReturn(response);

        // When
        Page<ProductResponse> result = productService.getProducts(0, 10, null, null, "createdAt", "desc");

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("deleteProduct: soft-deletes by setting deletedAt")
    void deleteProduct_shouldSetDeletedAt() {
        // Given
        Product product = Product.builder()
                .id(1L).name("To Delete")
                .price(BigDecimal.TEN).stock(10).build();

        when(productRepository.findActiveById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        // When
        productService.deleteProduct(1L);

        // Then
        assertThat(product.getDeletedAt()).isNotNull();
        verify(productRepository).save(product);
    }
}
