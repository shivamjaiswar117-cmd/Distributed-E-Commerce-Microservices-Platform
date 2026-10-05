package com.distributed.ecommerce.order.service;

import com.distributed.ecommerce.order.entity.Order;
import com.distributed.ecommerce.order.kafka.OrderCreatedEvent;
import com.distributed.ecommerce.order.kafka.OrderEventProducer;
import com.distributed.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderService orderService;

    private Order inputOrder;

    @BeforeEach
    void setUp() {
        inputOrder = new Order(1L, 2L, 3, new BigDecimal("999.00"), "PENDING");
    }

    @Test
    void createOrder_shouldSetStatusToPending() {
        Order savedOrder = new Order(1L, 2L, 3, new BigDecimal("999.00"), "PENDING");
        savedOrder.setId(10L);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        Order result = orderService.createOrder(inputOrder);

        assertEquals("PENDING", result.getStatus());
        assertEquals(10L, result.getId());
    }

    @Test
    void createOrder_shouldPublishOrderCreatedEvent() {
        Order savedOrder = new Order(1L, 2L, 3, new BigDecimal("999.00"), "PENDING");
        savedOrder.setId(10L);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        orderService.createOrder(inputOrder);

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventProducer, times(1)).publishOrderCreatedEvent(eventCaptor.capture());

        OrderCreatedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(10L, capturedEvent.getOrderId());
        assertEquals(1L, capturedEvent.getUserId());
        assertEquals(2L, capturedEvent.getProductId());
        assertEquals(3, capturedEvent.getQuantity());
        assertEquals(new BigDecimal("999.00"), capturedEvent.getTotalPrice());
    }

    @Test
    void getAllOrders_shouldReturnListFromRepository() {
        Order order1 = new Order(1L, 2L, 3, new BigDecimal("999.00"), "PENDING");
        Order order2 = new Order(4L, 5L, 6, new BigDecimal("500.00"), "PENDING");

        when(orderRepository.findAll()).thenReturn(List.of(order1, order2));

        List<Order> result = orderService.getAllOrders();

        assertEquals(2, result.size());
        verify(orderRepository, times(1)).findAll();
    }
}