package com.distributed.ecommerce.payment.kafka;

import com.distributed.ecommerce.payment.entity.Payment;
import com.distributed.ecommerce.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    @Autowired
    private PaymentRepository paymentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "order-created", groupId = "payment-service-group")
    public void handleOrderCreated(ConsumerRecord<String, String> record) {
        try {
            String json = record.value();
            System.out.println("Payment Service received event: " + json);

            OrderCreatedEvent event = objectMapper.readValue(json, OrderCreatedEvent.class);

            Payment payment = new Payment(
                    event.getOrderId(),
                    event.getUserId(),
                    event.getTotalPrice(),
                    "SUCCESS"
            );
            paymentRepository.save(payment);

            System.out.println("Payment record created for order id=" + event.getOrderId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}