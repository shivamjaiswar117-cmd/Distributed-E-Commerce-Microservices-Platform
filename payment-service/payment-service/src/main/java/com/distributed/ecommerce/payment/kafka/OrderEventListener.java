package com.distributed.ecommerce.payment.kafka;

import com.distributed.ecommerce.payment.entity.Payment;
import com.distributed.ecommerce.payment.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private final PaymentRepository paymentRepository;
    private final PaymentResultProducer paymentResultProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrderEventListener(
            PaymentRepository paymentRepository,
            PaymentResultProducer paymentResultProducer) {
        this.paymentRepository = paymentRepository;
        this.paymentResultProducer = paymentResultProducer;
    }

    @KafkaListener(topics = "order-created", groupId = "payment-service-group")
    public void handleOrderCreated(ConsumerRecord<String, String> record) {
        try {
            String json = record.value();
            System.out.println("Payment Service received event: " + json);

            OrderCreatedEvent event = objectMapper.readValue(
                    json, OrderCreatedEvent.class);

            Payment payment = new Payment(
                    event.getOrderId(),
                    event.getUserId(),
                    event.getTotalPrice(),
                    "SUCCESS"
            );

            paymentRepository.save(payment);
            System.out.println("Payment record created for order id=" + event.getOrderId());

            PaymentResultEvent result = new PaymentResultEvent(
                    event.getOrderId(), "SUCCESS");

            paymentResultProducer.publishPaymentResult(result);

                } catch (Exception e) {
            System.err.println(
                    "Failed to process order-created event: " + record.value());
            throw new IllegalStateException(
                    "Payment event processing failed", e);
        }
    }
}
