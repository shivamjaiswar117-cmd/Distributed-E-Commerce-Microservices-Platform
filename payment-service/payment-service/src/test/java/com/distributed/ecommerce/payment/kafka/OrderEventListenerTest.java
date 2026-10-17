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
import java.util.Optional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentResultProducer paymentResultProducer;

    @InjectMocks
    private OrderEventListener orderEventListener;

   @Test
void handleOrderCreated_shouldSavePaymentWithCorrectData() {
    String json = "{\"orderId\":42,\"userId\":7,\"productId\":3,\"quantity\":2,\"totalPrice\":50000}";

    ConsumerRecord<String, String> record =
            new ConsumerRecord<>("order-created", 0, 0L, "key", json);

    when(paymentRepository.findByOrderId(42L))
            .thenReturn(Optional.empty());

    when(paymentRepository.save(any(Payment.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    orderEventListener.handleOrderCreated(record);

    ArgumentCaptor<Payment> paymentCaptor =
            ArgumentCaptor.forClass(Payment.class);

    verify(paymentRepository, times(1)).save(paymentCaptor.capture());

    Payment savedPayment = paymentCaptor.getValue();

    assertEquals(42L, savedPayment.getOrderId());
    assertEquals(7L, savedPayment.getUserId());
    assertEquals(new BigDecimal("50000"), savedPayment.getAmount());
    assertEquals("SUCCESS", savedPayment.getStatus());

    verify(paymentResultProducer, times(1))
            .publishPaymentResult(any(PaymentResultEvent.class));
}

@Test
void handleOrderCreated_shouldReuseExistingPaymentForDuplicateOrder() {
    String json = "{\"orderId\":42,\"userId\":7,\"productId\":3,\"quantity\":2,\"totalPrice\":50000}";

    ConsumerRecord<String, String> record =
            new ConsumerRecord<>("order-created", 0, 0L, "key", json);

    Payment existingPayment =
            new Payment(42L, 7L, new BigDecimal("50000"), "SUCCESS");

    when(paymentRepository.findByOrderId(42L))
            .thenReturn(Optional.of(existingPayment));

    orderEventListener.handleOrderCreated(record);

    verify(paymentRepository, times(1)).findByOrderId(42L);
    verify(paymentRepository, times(0)).save(any(Payment.class));

    verify(paymentResultProducer, times(1))
            .publishPaymentResult(any(PaymentResultEvent.class));
}

}