package com.mypay.upi.npci.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "transactions")
public class Transaction {
    @Id
    private String id;
    private String fromUserId;
    private String toUserId;
    private String toUpiId;
    private String senderUpiId;
    private String receiverUpiId;
    private double amount;
    private String status;
    private Date timestamp;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSenderUpiId() { return senderUpiId; }
    public void setSenderUpiId(String senderUpiId) { this.senderUpiId = senderUpiId; }
    public String getReceiverUpiId() { return receiverUpiId; }
    public void setReceiverUpiId(String receiverUpiId) { this.receiverUpiId = receiverUpiId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }
    public String getToUpiId() { return toUpiId; }
    public void setToUpiId(String toUpiId) {
        this.toUpiId = toUpiId;
    }

}