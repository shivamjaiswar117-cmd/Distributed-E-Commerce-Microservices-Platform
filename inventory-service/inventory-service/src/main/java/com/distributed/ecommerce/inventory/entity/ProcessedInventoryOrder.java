package com.distributed.ecommerce.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "processed_inventory_orders")
public class ProcessedInventoryOrder {

    @Id
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    public ProcessedInventoryOrder() {
    }

    public ProcessedInventoryOrder(Long orderId) {
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
}