package com.distributed.ecommerce.inventory.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.TimeUnit;

@Service
public class InventoryResultProducer {

    private static final String TOPIC = "inventory-result";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public InventoryResultProducer(
            KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishInventoryResult(InventoryResultEvent event) {
        try {
            String json = jsonMapper.writeValueAsString(event);

            kafkaTemplate.send(
                TOPIC,
                String.valueOf(event.getOrderId()),
                json
            ).get(10, TimeUnit.SECONDS);

            System.out.println("Published inventory result: " + json);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                "Interrupted while publishing inventory result", e
            );
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to publish inventory result", e
            );
        }
    }
}