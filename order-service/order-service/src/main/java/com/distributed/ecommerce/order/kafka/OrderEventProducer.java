package com.distributed.ecommerce.order.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class OrderEventProducer {

    private static final String TOPIC = "order-created";
    private static final long SEND_TIMEOUT_SECONDS = 10;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public OrderEventProducer(
            KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = JsonMapper.builder().build();
    }

    public void publishOrderCreatedEvent(OrderCreatedEvent event) {
        try {
            String json = jsonMapper.writeValueAsString(event);

            kafkaTemplate.send(TOPIC, json)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while publishing order-created event", e);

        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException(
                    "Failed to publish order-created event to Kafka", e);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to serialize or publish order-created event", e);
        }
    }
}
