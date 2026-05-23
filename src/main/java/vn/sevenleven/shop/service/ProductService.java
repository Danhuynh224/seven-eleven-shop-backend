package vn.sevenleven.shop.service;

import org.springframework.data.domain.Page;
import vn.sevenleven.shop.dto.request.CreateProductRequest;
import vn.sevenleven.shop.dto.request.UpdateProductRequest;
import vn.sevenleven.shop.dto.response.ProductResponse;

public interface ProductService {

    Page<ProductResponse> getProducts(int page, int size, String search,
                                      Long categoryId, String sortBy, String direction);

    ProductResponse getProduct(Long id);

    ProductResponse createProduct(CreateProductRequest request);

    ProductResponse updateProduct(Long id, UpdateProductRequest request);

    void deleteProduct(Long id);
}
