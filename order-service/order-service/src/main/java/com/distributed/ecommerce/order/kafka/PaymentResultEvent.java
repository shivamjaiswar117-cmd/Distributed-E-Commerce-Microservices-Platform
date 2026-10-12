package com.distributed.ecommerce.order.kafka;

public class PaymentResultEvent {

    private Long orderId;
    private String status;

    public PaymentResultEvent() {
    }

    public PaymentResultEvent(Long orderId, String status) {
        this.orderId = orderId;
        this.status = status;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
