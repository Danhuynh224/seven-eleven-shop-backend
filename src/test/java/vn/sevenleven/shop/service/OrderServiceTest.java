package vn.sevenleven.shop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.sevenleven.shop.dto.request.CreateOrderRequest;
import vn.sevenleven.shop.dto.request.OrderItemRequest;
import vn.sevenleven.shop.dto.response.OrderResponse;
import vn.sevenleven.shop.entity.Order;
import vn.sevenleven.shop.entity.Product;
import vn.sevenleven.shop.entity.User;
import vn.sevenleven.shop.enums.OrderStatus;
import vn.sevenleven.shop.enums.Role;
import vn.sevenleven.shop.exception.BusinessException;
import vn.sevenleven.shop.exception.InsufficientStockException;
import vn.sevenleven.shop.exception.ResourceNotFoundException;
import vn.sevenleven.shop.mapper.OrderMapper;
import vn.sevenleven.shop.repository.OrderRepository;
import vn.sevenleven.shop.repository.ProductRepository;
import vn.sevenleven.shop.repository.UserRepository;
import vn.sevenleven.shop.service.impl.OrderServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User testUser;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("customer1")
                .role(Role.CUSTOMER)
                .fullName("Test Customer")
                .build();

        testProduct = Product.builder()
                .id(1L)
                .name("Test Product")
                .price(new BigDecimal("50000"))
                .stock(10)
                .build();
    }

    @Test
    @DisplayName("createOrder: success — stock is decremented and order is returned")
    void createOrder_success_shouldReturnOrderAndDecreaseStock() {
        // Given
        OrderItemRequest itemReq = new OrderItemRequest(1L, 3);
        CreateOrderRequest request = new CreateOrderRequest(List.of(itemReq), "test note");

        Order savedOrder = Order.builder()
                .id(1L)
                .user(testUser)
                .totalAmount(new BigDecimal("150000"))
                .status(OrderStatus.PENDING)
                .note("test note")
                .items(new ArrayList<>())
                .build();

        OrderResponse expectedResponse = new OrderResponse(
                1L, 1L, "customer1", new BigDecimal("150000"),
                "PENDING", "test note", List.of(),
                LocalDateTime.now(), LocalDateTime.now());

        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(testUser));
        when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testProduct));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(expectedResponse);

        // When
        OrderResponse result = orderService.createOrder(request, "customer1");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.totalAmount()).isEqualByComparingTo("150000");
        assertThat(testProduct.getStock()).isEqualTo(7); // 10 - 3 = 7
        verify(orderRepository).save(any(Order.class));
        verify(productRepository).findByIdWithLock(1L);
    }

    @Test
    @DisplayName("createOrder: insufficient stock — throws InsufficientStockException, no order saved")
    void createOrder_whenStockInsufficient_shouldThrowInsufficientStockException() {
        // Given — request quantity (5) exceeds available stock (2)
        testProduct.setStock(2);
        OrderItemRequest itemReq = new OrderItemRequest(1L, 5);
        CreateOrderRequest request = new CreateOrderRequest(List.of(itemReq), null);

        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(testUser));
        when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testProduct));

        // When / Then
        assertThrows(InsufficientStockException.class,
                () -> orderService.createOrder(request, "customer1"));

        verify(orderRepository, never()).save(any());
        assertThat(testProduct.getStock()).isEqualTo(2); // stock unchanged
    }

    @Test
    @DisplayName("createOrder: product not found — throws ResourceNotFoundException")
    void createOrder_whenProductNotFound_shouldThrowResourceNotFoundException() {
        // Given
        OrderItemRequest itemReq = new OrderItemRequest(999L, 1);
        CreateOrderRequest request = new CreateOrderRequest(List.of(itemReq), null);

        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(testUser));
        when(productRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(ResourceNotFoundException.class,
                () -> orderService.createOrder(request, "customer1"));

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOrder: empty items list — throws BusinessException before DB access")
    void createOrder_whenEmptyItems_shouldThrowValidationException() {
        // Given
        CreateOrderRequest request = new CreateOrderRequest(List.of(), null);

        // When / Then
        assertThrows(BusinessException.class,
                () -> orderService.createOrder(request, "customer1"));

        // User lookup should not happen when items are empty
        verify(userRepository, never()).findByUsername(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOrder: multiple items — total amount is sum of all subtotals")
    void createOrder_withMultipleItems_shouldCalculateTotalCorrectly() {
        // Given
        Product product2 = Product.builder()
                .id(2L)
                .name("Product 2")
                .price(new BigDecimal("30000"))
                .stock(5)
                .build();

        OrderItemRequest item1 = new OrderItemRequest(1L, 2); // 50000 * 2 = 100000
        OrderItemRequest item2 = new OrderItemRequest(2L, 3); // 30000 * 3 = 90000
        CreateOrderRequest request = new CreateOrderRequest(List.of(item1, item2), null);

        Order savedOrder = Order.builder()
                .id(1L).user(testUser)
                .totalAmount(new BigDecimal("190000"))
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        OrderResponse expectedResponse = new OrderResponse(
                1L, 1L, "customer1", new BigDecimal("190000"),
                "PENDING", null, List.of(),
                LocalDateTime.now(), LocalDateTime.now());

        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(testUser));
        when(productRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.findByIdWithLock(2L)).thenReturn(Optional.of(product2));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(expectedResponse);

        // When
        OrderResponse result = orderService.createOrder(request, "customer1");

        // Then
        assertThat(result.totalAmount()).isEqualByComparingTo("190000");
        assertThat(testProduct.getStock()).isEqualTo(8);  // 10 - 2
        assertThat(product2.getStock()).isEqualTo(2);     // 5 - 3
    }
}
