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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProcessedInventoryOrderRepository processedOrderRepository;

    @Mock
    private InventoryResultProducer inventoryResultProducer;

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

        verify(inventoryResultProducer).publishInventoryResult(
                argThat(event ->
                        event.getOrderId().equals(1L)
                                && event.getStatus().equals("SUCCESS"))
        );
    }

    @Test
    void handleOrderCreated_shouldIgnoreDuplicateOrder() {
        when(processedOrderRepository.existsById(1L)).thenReturn(true);

        assertDoesNotThrow(() -> orderEventListener.handleOrderCreated(record));

        verify(inventoryRepository, never()).findByProductId(any());
        verify(inventoryRepository, never()).save(any());
        verify(processedOrderRepository, never()).save(any());

        verify(inventoryResultProducer).publishInventoryResult(
                argThat(event ->
                        event.getOrderId().equals(1L)
                                && event.getStatus().equals("SUCCESS"))
        );
    }

    @Test
    void handleOrderCreated_shouldPublishFailureWhenInventoryIsMissing() {
        when(processedOrderRepository.existsById(1L)).thenReturn(false);
        when(inventoryRepository.findByProductId(5L))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> orderEventListener.handleOrderCreated(record));

        verify(inventoryResultProducer).publishInventoryResult(
                argThat(event ->
                        event.getOrderId().equals(1L)
                                && event.getStatus().equals("FAILED"))
        );

        verify(inventoryRepository, never()).save(any());
        verify(processedOrderRepository, never()).save(any());
    }

    @Test
    void handleOrderCreated_shouldPublishFailureWhenStockIsInsufficient() {
        Inventory inventory = new Inventory(5L, 5);

        when(processedOrderRepository.existsById(1L)).thenReturn(false);
        when(inventoryRepository.findByProductId(5L))
                .thenReturn(Optional.of(inventory));

        assertDoesNotThrow(() -> orderEventListener.handleOrderCreated(record));

        assertEquals(5, inventory.getAvailableQuantity());

        verify(inventoryResultProducer).publishInventoryResult(
                argThat(event ->
                        event.getOrderId().equals(1L)
                                && event.getStatus().equals("FAILED"))
        );

        verify(inventoryRepository, never()).save(any());
        verify(processedOrderRepository, never()).save(any());
    }

    @Test
    void handleOrderCreated_shouldPublishFailureForZeroQuantity() {
        ConsumerRecord<String, String> invalidRecord =
                new ConsumerRecord<>(
                        "order-created",
                        0,
                        1L,
                        null,
                        "{\"orderId\":2,\"userId\":1,\"productId\":5,\"quantity\":0,\"totalPrice\":1000}"
                );

        assertDoesNotThrow(
                () -> orderEventListener.handleOrderCreated(invalidRecord));

        verify(inventoryResultProducer).publishInventoryResult(
                argThat(event ->
                        event.getOrderId().equals(2L)
                                && event.getStatus().equals("FAILED"))
        );

        verifyNoInteractions(inventoryRepository, processedOrderRepository);
    }

    @Test
    void handleOrderCreated_shouldPublishFailureForNegativeQuantity() {
        ConsumerRecord<String, String> invalidRecord =
                new ConsumerRecord<>(
                        "order-created",
                        0,
                        1L,
                        null,
                        "{\"orderId\":2,\"userId\":1,\"productId\":5,\"quantity\":-1,\"totalPrice\":1000}"
                );

        assertDoesNotThrow(
                () -> orderEventListener.handleOrderCreated(invalidRecord));

        verify(inventoryResultProducer).publishInventoryResult(
                argThat(event ->
                        event.getOrderId().equals(2L)
                                && event.getStatus().equals("FAILED"))
        );

        verifyNoInteractions(inventoryRepository, processedOrderRepository);
    }

    @Test
    void handleOrderCreated_shouldRejectMissingOrderId() {
        assertInvalidEvent(
                "{\"userId\":1,\"productId\":5,\"quantity\":10,\"totalPrice\":1000}"
        );
    }

    @Test
    void handleOrderCreated_shouldPublishFailureWhenProductIdIsMissing() {
        ConsumerRecord<String, String> invalidRecord =
                new ConsumerRecord<>(
                        "order-created",
                        0,
                        1L,
                        null,
                        "{\"orderId\":2,\"userId\":1,\"quantity\":10,\"totalPrice\":1000}"
                );

        assertDoesNotThrow(
                () -> orderEventListener.handleOrderCreated(invalidRecord));

        verify(inventoryResultProducer).publishInventoryResult(
                argThat(event ->
                        event.getOrderId().equals(2L)
                                && event.getStatus().equals("FAILED"))
        );

        verifyNoInteractions(inventoryRepository, processedOrderRepository);
    }

    @Test
    void handleOrderCreated_shouldRejectMalformedJson() {
        assertInvalidEvent("{invalid-json}");
    }

    private void assertInvalidEvent(String payload) {
        ConsumerRecord<String, String> invalidRecord =
                new ConsumerRecord<>(
                        "order-created",
                        0,
                        1L,
                        null,
                        payload
                );

        assertThrows(
                RuntimeException.class,
                () -> orderEventListener.handleOrderCreated(invalidRecord)
        );

        verifyNoInteractions(inventoryRepository, processedOrderRepository);
        verifyNoInteractions(inventoryResultProducer);
    }
}