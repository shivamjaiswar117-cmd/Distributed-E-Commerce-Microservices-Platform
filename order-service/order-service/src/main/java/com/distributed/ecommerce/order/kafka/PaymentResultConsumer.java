package com.distributed.ecommerce.order.kafka;

import com.distributed.ecommerce.order.entity.Order;
import com.distributed.ecommerce.order.repository.OrderRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.Locale;

@Component
public class PaymentResultConsumer {

    private final OrderRepository orderRepository;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public PaymentResultConsumer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @KafkaListener(
        topics = "payment-result",
        groupId = "order-payment-result-group"
    )
    @Transactional
    public void handlePaymentResult(ConsumerRecord<String, String> record) {
        PaymentResultEvent event;

        try {
            event = jsonMapper.readValue(
                record.value(), PaymentResultEvent.class
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid payment result event", e);
        }

        if (event.getOrderId() == null || event.getStatus() == null) {
            throw new IllegalArgumentException(
                "Payment result requires orderId and status"
            );
        }

        String result = event.getStatus().toUpperCase(Locale.ROOT);

        if (!result.equals("SUCCESS") && !result.equals("FAILED")) {
            throw new IllegalArgumentException(
                "Unsupported payment status: " + result
            );
        }

        Order order = orderRepository.findByIdForUpdate(event.getOrderId())
            .orElseThrow(() -> new IllegalStateException(
                "Order not found: " + event.getOrderId()
            ));


        order.setPaymentStatus(result);
        updateOrderStatus(order);
        orderRepository.save(order);

        System.out.println(
            "Payment result processed for order #" + order.getId()
                + "; payment=" + result
                + "; inventory=" + order.getInventoryStatus()
                + "; order=" + order.getStatus()
        );
    }

    static void updateOrderStatus(Order order) {
        if ("FAILED".equals(order.getPaymentStatus())
                || "FAILED".equals(order.getInventoryStatus())) {
            order.setStatus("CANCELLED");
        } else if ("SUCCESS".equals(order.getPaymentStatus())
                && "SUCCESS".equals(order.getInventoryStatus())) {
            order.setStatus("CONFIRMED");
        } else {
            order.setStatus("PENDING");
        }
    }
}