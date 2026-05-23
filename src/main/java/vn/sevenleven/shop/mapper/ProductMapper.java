package vn.sevenleven.shop.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import vn.sevenleven.shop.dto.request.CreateProductRequest;
import vn.sevenleven.shop.dto.request.UpdateProductRequest;
import vn.sevenleven.shop.dto.response.ProductResponse;
import vn.sevenleven.shop.entity.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "categoryId",   source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    ProductResponse toResponse(Product product);

    // createdAt/updatedAt are inherited from BaseEntity — not in Lombok builder, ignored automatically
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "category",  ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Product toEntity(CreateProductRequest request);

    // Updates only non-null fields on the existing entity (category is handled in service)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "category",  ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntity(UpdateProductRequest request, @MappingTarget Product product);
}
