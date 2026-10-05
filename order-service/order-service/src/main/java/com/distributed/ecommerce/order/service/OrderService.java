package com.distributed.ecommerce.order.service;

import com.distributed.ecommerce.order.entity.Order;
import com.distributed.ecommerce.order.kafka.OrderCreatedEvent;
import com.distributed.ecommerce.order.kafka.OrderEventProducer;
import com.distributed.ecommerce.order.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderEventProducer orderEventProducer;

    public Order createOrder(Order order) {
        order.setStatus("PENDING");
        Order savedOrder = orderRepository.save(order);

        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getProductId(),
                savedOrder.getQuantity(),
                savedOrder.getTotalPrice()
        );

        orderEventProducer.publishOrderCreatedEvent(event);

        return savedOrder;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }
}