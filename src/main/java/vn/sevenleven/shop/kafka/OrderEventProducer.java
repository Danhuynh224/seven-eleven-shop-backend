package vn.sevenleven.shop.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import vn.sevenleven.shop.event.OrderCreatedEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    public static final String TOPIC = "order.created";

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            kafkaTemplate.send(TOPIC, String.valueOf(event.orderId()), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish OrderCreatedEvent orderId={}: {}",
                                    event.orderId(), ex.getMessage());
                        } else {
                            log.info("Published OrderCreatedEvent: orderId={}, partition={}, offset={}",
                                    event.orderId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (Exception e) {
            // Fire-and-forget: Kafka failure must not fail the order transaction
            log.error("Error sending OrderCreatedEvent orderId={}: {}", event.orderId(), e.getMessage());
        }
    }
}
