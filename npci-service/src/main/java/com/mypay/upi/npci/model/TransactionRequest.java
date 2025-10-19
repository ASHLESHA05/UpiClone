package com.xai.upi.client.model;

public class TransactionRequest {
    private String senderUpiId;
    private String receiverPhone;
    private double amount;
    private String upiPin;

    // Getters and Setters
    public String getSenderUpiId() { return senderUpiId; }
    public void setSenderUpiId(String senderUpiId) { this.senderUpiId = senderUpiId; }
    public String getReceiverPhone() { return receiverPhone; }
    public void setReceiverPhone(String receiverPhone) { this.receiverPhone = receiverPhone; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getUpiPin() { return upiPin; }
    public void setUpiPin(String upiPin) { this.upiPin = upiPin; }
}