package com.distributed.ecommerce.order.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class OrderEventProducer {

    private static final String TOPIC = "order-created";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public void publishOrderCreatedEvent(OrderCreatedEvent event) {
        try {
            String json = jsonMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}