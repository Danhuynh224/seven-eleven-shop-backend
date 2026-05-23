package vn.sevenleven.shop.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import vn.sevenleven.shop.kafka.OrderEventProducer;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(OrderEventProducer.TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
