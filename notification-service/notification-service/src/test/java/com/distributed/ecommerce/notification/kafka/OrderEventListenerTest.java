package com.distributed.ecommerce.notification.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderEventListenerTest {

    @Test
    void handleOrderCreated_shouldPrintCorrectNotificationMessage() {
        OrderEventListener listener = new OrderEventListener();

        String json = "{\"orderId\":15,\"userId\":9,\"productId\":2,\"quantity\":1,\"totalPrice\":50000}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("order-created", 0, 0L, "key", json);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        listener.handleOrderCreated(record);

        System.setOut(originalOut);

        String printedOutput = outputStream.toString();
        assertTrue(printedOutput.contains("Order #15"));
        assertTrue(printedOutput.contains("userId=9"));
        assertTrue(printedOutput.contains("total=50000"));
    }
}