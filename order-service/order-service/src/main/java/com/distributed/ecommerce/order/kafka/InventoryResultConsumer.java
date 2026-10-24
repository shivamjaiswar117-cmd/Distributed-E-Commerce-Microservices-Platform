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
public class InventoryResultConsumer {

    private final OrderRepository orderRepository;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public InventoryResultConsumer(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @KafkaListener(
        topics = "inventory-result",
        groupId = "order-inventory-result-group"
    )
    @Transactional
    public void handleInventoryResult(ConsumerRecord<String, String> record) {
        try {
            InventoryResultEvent event = jsonMapper.readValue(
                record.value(), InventoryResultEvent.class
            );

            if (event.getOrderId() == null || event.getStatus() == null) {
                throw new IllegalArgumentException(
                    "Inventory result requires orderId and status"
                );
            }

            String result = event.getStatus().toUpperCase(Locale.ROOT);

            if (!result.equals("SUCCESS") && !result.equals("FAILED")) {
                throw new IllegalArgumentException(
                    "Unsupported inventory status: " + result
                );
            }

            Order order = orderRepository.findByIdForUpdate(event.getOrderId())
                .orElseThrow(() -> new IllegalStateException(
                    "Order not found: " + event.getOrderId()
                ));



            order.setInventoryStatus(result);
            PaymentResultConsumer.updateOrderStatus(order);
            orderRepository.save(order);

            System.out.println(
                "Inventory result processed for order #" + order.getId()
                    + "; payment=" + order.getPaymentStatus()
                    + "; inventory=" + result
                    + "; order=" + order.getStatus()
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                "Inventory result processing failed: " + record.value(), e
            );
        }
    }
}
