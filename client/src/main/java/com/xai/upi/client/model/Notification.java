package com.xai.upi.client.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;
    private String userId; // Receiver's user ID (who needs to pay)
    private String requesterUserId; // Sender's user ID (who requested)
    private String requesterUpiId; // Sender's UPI ID
    private String requesterName; // Sender's name
    private double amount;
    private String status; // PENDING, COMPLETED, CANCELLED
    private LocalDateTime createdAt; // Timestamp for request creation

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getRequesterUserId() { return requesterUserId; }
    public void setRequesterUserId(String requesterUserId) { this.requesterUserId = requesterUserId; }
    public String getRequesterUpiId() { return requesterUpiId; }
    public void setRequesterUpiId(String requesterUpiId) { this.requesterUpiId = requesterUpiId; }
    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // Helper for formattedDate
    public String getFormattedDate() {
        if (createdAt == null) return "";
        return createdAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
    }
}