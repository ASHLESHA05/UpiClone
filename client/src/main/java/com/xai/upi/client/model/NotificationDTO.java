package com.xai.upi.client.model;

public class NotificationDTO {
    private String id;
    private String sender; // Maps to requesterName or requesterUpiId
    private String receiver; // Maps to userId
    private double amount;
    private String description; // Static or derived
    private String formattedDate; // From createdAt

    // Constructor to map from Notification
    public NotificationDTO(Notification notification) {
        this.id = notification.getId();
        this.sender = notification.getRequesterName() != null ? notification.getRequesterName() : notification.getRequesterUpiId();
        this.receiver = notification.getUserId();
        this.amount = notification.getAmount();
        this.description = "Money Request from " + this.sender;
        this.formattedDate = notification.getFormattedDate();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFormattedDate() { return formattedDate; }
    public void setFormattedDate(String formattedDate) { this.formattedDate = formattedDate; }
}