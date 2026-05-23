package vn.sevenleven.shop.service;

import org.springframework.data.domain.Page;
import vn.sevenleven.shop.dto.request.CreateOrderRequest;
import vn.sevenleven.shop.dto.request.UpdateOrderStatusRequest;
import vn.sevenleven.shop.dto.response.OrderResponse;
import vn.sevenleven.shop.enums.OrderStatus;

import java.time.LocalDateTime;

public interface OrderService {

    OrderResponse createOrder(CreateOrderRequest request, String username);

    Page<OrderResponse> getMyOrders(String username, int page, int size);

    Page<OrderResponse> getAllOrders(OrderStatus status, LocalDateTime from,
                                     LocalDateTime to, int page, int size);

    OrderResponse getOrderById(Long id);

    OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request);
}
