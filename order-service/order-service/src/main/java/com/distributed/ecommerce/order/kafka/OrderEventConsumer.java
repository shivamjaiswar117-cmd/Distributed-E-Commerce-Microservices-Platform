package com.distributed.ecommerce.order.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventConsumer {

    @KafkaListener(topics = "order-created", groupId = "order-service-group")
    public void consumeOrderCreatedEvent(ConsumerRecord<String, String> record) {
        System.out.println("Consumed event from topic 'order-created': " + record.value());
    }
}