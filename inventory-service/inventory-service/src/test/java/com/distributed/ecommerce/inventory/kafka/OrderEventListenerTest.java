package com.distributed.ecommerce.inventory.kafka;

import com.distributed.ecommerce.inventory.entity.Inventory;
import com.distributed.ecommerce.inventory.repository.InventoryRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private OrderEventListener orderEventListener;

    @Test
    void handleOrderCreated_shouldDecrementExistingStock() {
        String json = "{\"orderId\":1,\"userId\":1,\"productId\":5,\"quantity\":10,\"totalPrice\":1000}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("order-created", 0, 0L, "key", json);

        Inventory existingInventory = new Inventory(5L, 50);
        when(inventoryRepository.findByProductId(5L)).thenReturn(Optional.of(existingInventory));

        orderEventListener.handleOrderCreated(record);

        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository, times(1)).save(inventoryCaptor.capture());

        assertEquals(40, inventoryCaptor.getValue().getAvailableQuantity());
    }

    @Test
    void handleOrderCreated_shouldDefaultToHundredStock_whenProductNotFoundBefore() {
        String json = "{\"orderId\":2,\"userId\":1,\"productId\":99,\"quantity\":15,\"totalPrice\":2000}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("order-created", 0, 0L, "key", json);

        when(inventoryRepository.findByProductId(99L)).thenReturn(Optional.empty());

        orderEventListener.handleOrderCreated(record);

        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository, times(1)).save(inventoryCaptor.capture());

        assertEquals(85, inventoryCaptor.getValue().getAvailableQuantity());
    }

    @Test
    void handleOrderCreated_shouldNotGoBelowZero() {
        String json = "{\"orderId\":3,\"userId\":1,\"productId\":7,\"quantity\":999,\"totalPrice\":500}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("order-created", 0, 0L, "key", json);

        Inventory lowStock = new Inventory(7L, 5);
        when(inventoryRepository.findByProductId(7L)).thenReturn(Optional.of(lowStock));

        orderEventListener.handleOrderCreated(record);

        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository, times(1)).save(inventoryCaptor.capture());

        assertEquals(0, inventoryCaptor.getValue().getAvailableQuantity());
    }
}