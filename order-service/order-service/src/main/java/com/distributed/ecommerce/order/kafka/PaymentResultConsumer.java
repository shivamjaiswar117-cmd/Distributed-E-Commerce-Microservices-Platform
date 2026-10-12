package com.distributed.ecommerce.order.kafka;

import com.distributed.ecommerce.order.entity.Order;
import com.distributed.ecommerce.order.repository.OrderRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Component
public class PaymentResultConsumer {

    private final OrderRepository orderRepository;

    public PaymentResultConsumer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @KafkaListener(topics = "payment-result", groupId = "order-payment-result-group")
    @Transactional
    public void handlePaymentResult(ConsumerRecord<String, String> record) {
        try {
            PaymentResultEvent event = tools.jackson.databind.json.JsonMapper.builder()
                    .build()
                    .readValue(record.value(), PaymentResultEvent.class);

            if (event.getOrderId() == null || event.getStatus() == null) {
                throw new IllegalArgumentException("Payment result is missing orderId or status");
            }

            String paymentStatus = event.getStatus().toUpperCase(Locale.ROOT);

            Optional<Order> optionalOrder = orderRepository.findById(event.getOrderId());

            if (optionalOrder.isEmpty()) {
                throw new IllegalStateException("Order not found: " + event.getOrderId());
            }

            Order order = optionalOrder.get();

            if ("SUCCESS".equals(paymentStatus)) {
                order.setStatus("CONFIRMED");
            } else if ("FAILED".equals(paymentStatus)) {
                order.setStatus("CANCELLED");
            } else {
                throw new IllegalArgumentException("Unsupported payment status: " + paymentStatus);
            }

            orderRepository.save(order);

            System.out.println("Order #" + order.getId()
                    + " status updated to " + order.getStatus());

        } catch (Exception e) {
            System.err.println("Failed to process payment result: " + record.value());
            e.printStackTrace();
            throw new IllegalStateException("Payment result processing failed", e);
        }
    }
}
