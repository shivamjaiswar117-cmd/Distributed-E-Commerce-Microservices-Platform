package com.distributed.ecommerce.inventory.kafka;

import com.distributed.ecommerce.inventory.entity.Inventory;
import com.distributed.ecommerce.inventory.entity.ProcessedInventoryOrder;
import com.distributed.ecommerce.inventory.repository.InventoryRepository;
import com.distributed.ecommerce.inventory.repository.ProcessedInventoryOrderRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OrderEventListener {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProcessedInventoryOrderRepository processedOrderRepository;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @KafkaListener(topics = "order-created", groupId = "inventory-service-group")
    @Transactional
    public void handleOrderCreated(ConsumerRecord<String, String> record) {
        try {
            OrderCreatedEvent event =
                    jsonMapper.readValue(record.value(), OrderCreatedEvent.class);

            if (event.getOrderId() == null
                    || event.getProductId() == null
                    || event.getQuantity() == null
                    || event.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Order event must contain a valid orderId, productId and quantity");
            }

            if (processedOrderRepository.existsById(event.getOrderId())) {
                System.out.println(
                        "Duplicate order event ignored: " + event.getOrderId());
                return;
            }

            Inventory inventory = inventoryRepository
                    .findByProductId(event.getProductId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Inventory not initialized for productId="
                                    + event.getProductId()));

            if (inventory.getAvailableQuantity() < event.getQuantity()) {
                throw new IllegalStateException(
                        "Insufficient inventory for productId="
                                + event.getProductId());
            }

            inventory.setAvailableQuantity(
                    inventory.getAvailableQuantity() - event.getQuantity());

            inventoryRepository.save(inventory);

            processedOrderRepository.save(
                    new ProcessedInventoryOrder(event.getOrderId()));

            System.out.println(
                    "Inventory updated for productId=" + event.getProductId()
                            + ", remaining stock="
                            + inventory.getAvailableQuantity());

        } catch (Exception e) {
            System.err.println(
                    "Failed to process order-created event: " + record.value());
            throw new IllegalStateException(
                    "Inventory event processing failed", e);
        }
    }
}