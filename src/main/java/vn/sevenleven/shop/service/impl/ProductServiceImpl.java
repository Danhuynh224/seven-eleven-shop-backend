package vn.sevenleven.shop.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.sevenleven.shop.dto.request.CreateProductRequest;
import vn.sevenleven.shop.dto.request.UpdateProductRequest;
import vn.sevenleven.shop.dto.response.ProductResponse;
import vn.sevenleven.shop.entity.Category;
import vn.sevenleven.shop.entity.Product;
import vn.sevenleven.shop.exception.ResourceNotFoundException;
import vn.sevenleven.shop.mapper.ProductMapper;
import vn.sevenleven.shop.repository.CategoryRepository;
import vn.sevenleven.shop.repository.ProductRepository;
import vn.sevenleven.shop.service.ProductService;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private static final List<String> ALLOWED_SORT_FIELDS =
            List.of("price", "name", "createdAt", "stock");

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(int page, int size, String search,
                                             Long categoryId, String sortBy, String direction) {
        // Whitelist sort field to prevent injection
        String validSortBy = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort sort = "desc".equalsIgnoreCase(direction)
                ? Sort.by(validSortBy).descending()
                : Sort.by(validSortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String searchParam = StringUtils.hasText(search) ? search : null;

        return productRepository.findActiveProducts(searchParam, categoryId, pageable)
                .map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Product product = productMapper.toEntity(request);

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found with id: " + request.categoryId()));
            product.setCategory(category);
        }

        Product saved = productRepository.save(product);
        log.info("Product created: id={}, name={}", saved.getId(), saved.getName());
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product product = productRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        productMapper.updateEntity(request, product);

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found with id: " + request.categoryId()));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        Product saved = productRepository.save(product);
        log.info("Product updated: id={}", id);
        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setDeletedAt(LocalDateTime.now());
        productRepository.save(product);
        log.info("Product soft-deleted: id={}", id);
    }
}
