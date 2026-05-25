package vn.sevenleven.shop.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.sevenleven.shop.dto.request.CreateOrderRequest;
import vn.sevenleven.shop.dto.request.OrderItemRequest;
import vn.sevenleven.shop.dto.request.UpdateOrderStatusRequest;
import vn.sevenleven.shop.dto.response.OrderResponse;
import vn.sevenleven.shop.entity.Order;
import vn.sevenleven.shop.entity.OrderItem;
import vn.sevenleven.shop.entity.Product;
import vn.sevenleven.shop.entity.User;
import vn.sevenleven.shop.enums.OrderStatus;
import vn.sevenleven.shop.event.OrderCreatedEvent;
import vn.sevenleven.shop.exception.BusinessException;
import vn.sevenleven.shop.exception.InsufficientStockException;
import vn.sevenleven.shop.exception.ResourceNotFoundException;
import vn.sevenleven.shop.kafka.OrderEventProducer;
import vn.sevenleven.shop.mapper.OrderMapper;
import vn.sevenleven.shop.repository.OrderRepository;
import vn.sevenleven.shop.repository.ProductRepository;
import vn.sevenleven.shop.repository.UserRepository;
import vn.sevenleven.shop.service.OrderService;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final OrderEventProducer orderEventProducer;

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String username) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new BusinessException("Order must contain at least one item");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        Map<Long, Integer> quantityByProductId = mergeQuantitiesByProductId(request.items());

        for (Map.Entry<Long, Integer> itemReq : quantityByProductId.entrySet()) {
            Long productId = itemReq.getKey();
            Integer quantity = itemReq.getValue();
            // Pessimistic lock prevents concurrent threads from overselling the same product
            Product product = productRepository.findByIdWithLock(productId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + productId));

            if (product.getStock() < quantity) {
                throw new InsufficientStockException(
                        String.format("Insufficient stock for '%s'. Available: %d, requested: %d",
                                product.getName(), product.getStock(), quantity));
            }

            product.setStock(product.getStock() - quantity);

            // Snapshot price from DB — never trust client-supplied price
            BigDecimal unitPrice = product.getPrice();
            totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));

            orderItems.add(OrderItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .build());
        }

        Order order = Order.builder()
                .user(user)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .note(request.note())
                .build();

        for (OrderItem item : orderItems) {
            item.setOrder(order);
            order.getItems().add(item);
        }

        Order saved = orderRepository.save(order); // cascades to order_items
        log.info("Order created: id={}, user={}, items={}, total={}",
                saved.getId(), username, orderItems.size(), totalAmount);

        // Publish event after DB commit — fire-and-forget, Kafka failure does not roll back the order
        orderEventProducer.publishOrderCreated(new OrderCreatedEvent(
                saved.getId(), user.getId(), username,
                totalAmount, orderItems.size(), saved.getCreatedAt()));

        return orderMapper.toResponse(saved);
    }

    private Map<Long, Integer> mergeQuantitiesByProductId(List<OrderItemRequest> items) {
        Map<Long, Integer> quantityByProductId = new LinkedHashMap<>();
        for (OrderItemRequest item : items) {
            quantityByProductId.merge(item.productId(), item.quantity(), Integer::sum);
        }
        return quantityByProductId;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getMyOrders(String username, int page, int size) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Pageable pageable = PageRequest.of(page, size);
        return orderRepository.findByUserId(user.getId(), pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(OrderStatus status, LocalDateTime from,
                                            LocalDateTime to, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Order> spec = buildOrderFilters(status, from, to);

        return orderRepository.findAll(spec, pageable)
                .map(orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        order.setStatus(request.status());
        Order saved = orderRepository.save(order);
        log.info("Order status updated: id={}, status={}", id, request.status());
        return orderMapper.toResponse(saved);
    }

    private Specification<Order> buildOrderFilters(OrderStatus status, LocalDateTime from,
                                                   LocalDateTime to) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return predicates.isEmpty()
                    ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
