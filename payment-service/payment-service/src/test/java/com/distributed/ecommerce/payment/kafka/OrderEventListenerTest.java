package com.distributed.ecommerce.payment.kafka;

import com.distributed.ecommerce.payment.entity.Payment;
import com.distributed.ecommerce.payment.repository.PaymentRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private OrderEventListener orderEventListener;

    @Test
    void handleOrderCreated_shouldSavePaymentWithCorrectData() {
        String json = "{\"orderId\":42,\"userId\":7,\"productId\":3,\"quantity\":2,\"totalPrice\":50000}";
        ConsumerRecord<String, String> record = new ConsumerRecord<>("order-created", 0, 0L, "key", json);

        orderEventListener.handleOrderCreated(record);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());

        Payment savedPayment = paymentCaptor.getValue();
        assertEquals(42L, savedPayment.getOrderId());
        assertEquals(7L, savedPayment.getUserId());
        assertEquals(new BigDecimal("50000"), savedPayment.getAmount());
        assertEquals("SUCCESS", savedPayment.getStatus());
    }
}