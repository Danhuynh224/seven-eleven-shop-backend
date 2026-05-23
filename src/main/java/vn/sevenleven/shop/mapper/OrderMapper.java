package vn.sevenleven.shop.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import vn.sevenleven.shop.dto.response.OrderItemResponse;
import vn.sevenleven.shop.dto.response.OrderResponse;
import vn.sevenleven.shop.entity.Order;
import vn.sevenleven.shop.entity.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "userId",   source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "status",   expression = "java(order.getStatus().name())")
    OrderResponse toResponse(Order order);

    @Mapping(target = "productId",   source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "subtotal",
             expression = "java(orderItem.getUnitPrice().multiply(java.math.BigDecimal.valueOf(orderItem.getQuantity())))")
    OrderItemResponse toItemResponse(OrderItem orderItem);
}
