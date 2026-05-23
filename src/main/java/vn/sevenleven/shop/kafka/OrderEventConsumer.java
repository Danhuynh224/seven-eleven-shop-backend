package vn.sevenleven.shop.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import vn.sevenleven.shop.event.OrderCreatedEvent;

@Component
@Slf4j
public class OrderEventConsumer {

    @KafkaListener(topics = OrderEventProducer.TOPIC, groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("[Kafka] OrderCreatedEvent received — orderId={}, user={}, items={}, total={}",
                event.orderId(), event.username(), event.itemCount(), event.totalAmount());
        // Production use cases: push notification, analytics, inventory sync, loyalty points
    }
}
