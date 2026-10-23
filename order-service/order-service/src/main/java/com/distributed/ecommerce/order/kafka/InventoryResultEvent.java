package com.distributed.ecommerce.order.kafka;

public class InventoryResultEvent {

    private Long orderId;
    private String status;

    public InventoryResultEvent() {}

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}