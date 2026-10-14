package com.community.ecommerce.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @Column(length = 32)
    private String id;

    @Column(unique = true, length = 32)
    private String orderNo;

    private String userId;

    // PENDING_PAYMENT | PENDING_SHIPMENT | PENDING_RECEIPT | COMPLETED | CANCELLED | REFUNDING | REFUNDED
    private String status;

    private Double totalAmount;
    private Long createTime;
    private Long payTime;
    private Long shipTime;
    private Long finishTime;
    private String trackingNo;
    private String cancelReason;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }
    public Long getCreateTime() { return createTime; }
    public void setCreateTime(Long createTime) { this.createTime = createTime; }
    public Long getPayTime() { return payTime; }
    public void setPayTime(Long payTime) { this.payTime = payTime; }
    public Long getShipTime() { return shipTime; }
    public void setShipTime(Long shipTime) { this.shipTime = shipTime; }
    public Long getFinishTime() { return finishTime; }
    public void setFinishTime(Long finishTime) { this.finishTime = finishTime; }
    public String getTrackingNo() { return trackingNo; }
    public void setTrackingNo(String trackingNo) { this.trackingNo = trackingNo; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
}
