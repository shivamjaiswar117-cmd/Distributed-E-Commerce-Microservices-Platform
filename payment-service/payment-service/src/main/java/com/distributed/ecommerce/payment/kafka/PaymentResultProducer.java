package com.distributed.ecommerce.payment.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class PaymentResultProducer {

    private static final String TOPIC = "payment-result";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public PaymentResultProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishPaymentResult(PaymentResultEvent event) {
        try {
            String json = jsonMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, String.valueOf(event.getOrderId()), json);
            System.out.println("Published payment result: " + json);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish payment result", e);
        }
    }
}
