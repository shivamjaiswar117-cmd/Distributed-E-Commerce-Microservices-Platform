package com.distributed.ecommerce.payment.kafka;

import com.distributed.ecommerce.payment.entity.Payment;
import com.distributed.ecommerce.payment.repository.PaymentRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OrderEventListener {

    private final PaymentRepository paymentRepository;
    private final PaymentResultProducer paymentResultProducer;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public OrderEventListener(
            PaymentRepository paymentRepository,
            PaymentResultProducer paymentResultProducer) {
        this.paymentRepository = paymentRepository;
        this.paymentResultProducer = paymentResultProducer;
    }

    @KafkaListener(topics = "order-created", groupId = "payment-service-group")
    @Transactional
    public void handleOrderCreated(ConsumerRecord<String, String> record) {
        try {
            OrderCreatedEvent event =
                    jsonMapper.readValue(record.value(), OrderCreatedEvent.class);

            if (event.getOrderId() == null
                    || event.getUserId() == null
                    || event.getTotalPrice() == null
                    || event.getTotalPrice().signum() < 0) {
                throw new IllegalArgumentException(
                        "Order event must contain valid orderId, userId and totalPrice");
            }

            Payment payment = paymentRepository.findByOrderId(event.getOrderId())
                    .orElseGet(() -> paymentRepository.save(
                            new Payment(
                                    event.getOrderId(),
                                    event.getUserId(),
                                    event.getTotalPrice(),
                                    "SUCCESS")));

            PaymentResultEvent result =
                    new PaymentResultEvent(payment.getOrderId(), payment.getStatus());

            paymentResultProducer.publishPaymentResult(result);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Payment event processing failed: " + record.value(), e);
        }
    }
}