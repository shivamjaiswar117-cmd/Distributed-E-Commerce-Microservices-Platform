package com.distributed.ecommerce.inventory.kafka;

import com.distributed.ecommerce.inventory.entity.Inventory;
import com.distributed.ecommerce.inventory.repository.InventoryRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OrderEventListener {

    @Autowired
    private InventoryRepository inventoryRepository;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @KafkaListener(topics = "order-created", groupId = "inventory-service-group")
    public void handleOrderCreated(ConsumerRecord<String, String> record) {
        try {
            String json = record.value();
            System.out.println("Inventory Service received event: " + json);

            OrderCreatedEvent event = jsonMapper.readValue(json, OrderCreatedEvent.class);

            Inventory inventory = inventoryRepository.findByProductId(event.getProductId())
                    .orElse(new Inventory(event.getProductId(), 100)); // default stock if not seeded yet

            int updatedQuantity = inventory.getAvailableQuantity() - event.getQuantity();
            inventory.setAvailableQuantity(Math.max(updatedQuantity, 0)); // never go negative

            inventoryRepository.save(inventory);

            System.out.println("Inventory updated for productId=" + event.getProductId()
                    + ", remaining stock=" + inventory.getAvailableQuantity());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}