package com.distributed.ecommerce.inventory.kafka;

import com.distributed.ecommerce.inventory.entity.Inventory;
import com.distributed.ecommerce.inventory.entity.ProcessedInventoryOrder;
import com.distributed.ecommerce.inventory.repository.InventoryRepository;
import com.distributed.ecommerce.inventory.repository.ProcessedInventoryOrderRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OrderEventListener {

    private final InventoryRepository inventoryRepository;
    private final ProcessedInventoryOrderRepository processedOrderRepository;
    private final InventoryResultProducer inventoryResultProducer;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public OrderEventListener(
            InventoryRepository inventoryRepository,
            ProcessedInventoryOrderRepository processedOrderRepository,
            InventoryResultProducer inventoryResultProducer) {
        this.inventoryRepository = inventoryRepository;
        this.processedOrderRepository = processedOrderRepository;
        this.inventoryResultProducer = inventoryResultProducer;
    }

    @KafkaListener(topics = "order-created", groupId = "inventory-service-group")
    @Transactional
    public void handleOrderCreated(ConsumerRecord<String, String> record) {
        OrderCreatedEvent event;

        try {
            event = jsonMapper.readValue(
                record.value(), OrderCreatedEvent.class
            );
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Invalid order-created event: " + record.value(), e
            );
        }

        if (event.getOrderId() == null) {
            throw new IllegalArgumentException(
                "Order event must contain orderId"
            );
        }

        if (event.getProductId() == null
                || event.getQuantity() == null
                || event.getQuantity() <= 0) {
            inventoryResultProducer.publishInventoryResult(
                new InventoryResultEvent(event.getOrderId(), "FAILED")
            );
            return;
        }

        if (processedOrderRepository.existsById(event.getOrderId())) {
            inventoryResultProducer.publishInventoryResult(
                new InventoryResultEvent(event.getOrderId(), "SUCCESS")
            );
            return;
        }

        try {
            Inventory inventory = inventoryRepository
                .findByProductId(event.getProductId())
                .orElseThrow(() -> new IllegalStateException(
                    "Inventory not initialized for productId="
                        + event.getProductId()
                ));

            if (inventory.getAvailableQuantity() < event.getQuantity()) {
                throw new IllegalStateException(
                    "Insufficient inventory for productId="
                        + event.getProductId()
                );
            }

            inventory.setAvailableQuantity(
                inventory.getAvailableQuantity() - event.getQuantity()
            );

            inventoryRepository.save(inventory);
            processedOrderRepository.save(
                new ProcessedInventoryOrder(event.getOrderId())
            );

        } catch (RuntimeException e) {
            System.err.println(
                "Inventory reservation failed for order #"
                    + event.getOrderId() + ": " + e.getMessage()
            );

            inventoryResultProducer.publishInventoryResult(
                new InventoryResultEvent(event.getOrderId(), "FAILED")
            );
            return;
        }

        inventoryResultProducer.publishInventoryResult(
            new InventoryResultEvent(event.getOrderId(), "SUCCESS")
        );
    }
}