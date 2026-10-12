package com.distributed.ecommerce.notification.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OrderEventListener {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @KafkaListener(topics = "order-created", groupId = "notification-service-group")
    public void handleOrderCreated(ConsumerRecord<String, String> record) {
        try {
            OrderCreatedEvent event = jsonMapper.readValue(
                    record.value(), OrderCreatedEvent.class);

            System.out.println("Order #" + event.getOrderId()
                    + " received and awaiting payment processing for userId="
                    + event.getUserId()
                    + ", total=" + event.getTotalPrice());

        } catch (Exception e) {
            System.err.println("Failed to process order-created notification");
            e.printStackTrace();
            throw new IllegalStateException("Notification processing failed", e);
        }
    }
}
