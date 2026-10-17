package com.distributed.ecommerce.inventory.kafka;

import com.distributed.ecommerce.inventory.entity.Inventory;
import com.distributed.ecommerce.inventory.repository.InventoryRepository;
import com.distributed.ecommerce.inventory.repository.ProcessedInventoryOrderRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProcessedInventoryOrderRepository processedOrderRepository;

    @InjectMocks
    private OrderEventListener orderEventListener;

    private ConsumerRecord<String, String> record;

    @BeforeEach
    void setUp() {
        record = new ConsumerRecord<>(
                "order-created",
                0,
                0L,
                null,
                "{\"orderId\":1,\"userId\":1,\"productId\":5,\"quantity\":10,\"totalPrice\":1000}"
        );
    }

    @Test
    void handleOrderCreated_shouldDecrementExistingStock() {
        Inventory inventory = new Inventory(5L, 50);

        when(processedOrderRepository.existsById(1L)).thenReturn(false);
        when(inventoryRepository.findByProductId(5L))
                .thenReturn(Optional.of(inventory));

        assertDoesNotThrow(() -> orderEventListener.handleOrderCreated(record));

        assertEquals(40, inventory.getAvailableQuantity());
        verify(inventoryRepository).save(inventory);
        verify(processedOrderRepository)
                .save(argThat(order -> order.getOrderId().equals(1L)));
    }

    @Test
    void handleOrderCreated_shouldIgnoreDuplicateOrder() {
        when(processedOrderRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> orderEventListener.handleOrderCreated(record));

        verify(inventoryRepository, never()).findByProductId(any());
        verify(inventoryRepository, never()).save(any());
        verify(processedOrderRepository, never()).save(any());
    }

    @Test
    void handleOrderCreated_shouldRejectMissingInventory() {
        when(processedOrderRepository.existsById(1L)).thenReturn(false);
        when(inventoryRepository.findByProductId(5L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> orderEventListener.handleOrderCreated(record));

        verify(inventoryRepository, never()).save(any());
        verify(processedOrderRepository, never()).save(any());
    }

    @Test
    void handleOrderCreated_shouldRejectInsufficientStock() {
        Inventory inventory = new Inventory(5L, 5);

        when(processedOrderRepository.existsById(1L)).thenReturn(false);
        when(inventoryRepository.findByProductId(5L))
                .thenReturn(Optional.of(inventory));

        assertThrows(
                IllegalStateException.class,
                () -> orderEventListener.handleOrderCreated(record));

        assertEquals(5, inventory.getAvailableQuantity());
        verify(inventoryRepository, never()).save(any());
        verify(processedOrderRepository, never()).save(any());
    }

    @Test
    void handleOrderCreated_shouldRejectZeroQuantity() {
        assertInvalidEvent(
                "{\"orderId\":2,\"userId\":1,\"productId\":5,\"quantity\":0,\"totalPrice\":1000}");
    }

    @Test
    void handleOrderCreated_shouldRejectNegativeQuantity() {
        assertInvalidEvent(
                "{\"orderId\":2,\"userId\":1,\"productId\":5,\"quantity\":-1,\"totalPrice\":1000}");
    }

    @Test
    void handleOrderCreated_shouldRejectMissingOrderId() {
        assertInvalidEvent(
                "{\"userId\":1,\"productId\":5,\"quantity\":10,\"totalPrice\":1000}");
    }

    @Test
    void handleOrderCreated_shouldRejectMissingProductId() {
        assertInvalidEvent(
                "{\"orderId\":2,\"userId\":1,\"quantity\":10,\"totalPrice\":1000}");
    }

    @Test
    void handleOrderCreated_shouldRejectMalformedJson() {
        assertInvalidEvent("{invalid-json}");
    }

    private void assertInvalidEvent(String payload) {
        ConsumerRecord<String, String> invalidRecord =
                new ConsumerRecord<>("order-created", 0, 1L, null, payload);

        assertThrows(
                IllegalStateException.class,
                () -> orderEventListener.handleOrderCreated(invalidRecord));

        verifyNoInteractions(inventoryRepository, processedOrderRepository);
    }
}
