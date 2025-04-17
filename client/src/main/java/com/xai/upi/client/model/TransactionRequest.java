package com.xai.upi.client.model;

import lombok.Data;

@Data
public class TransactionRequest {
    private String senderUpiId;
    private String receiverPhone;
    private double amount;
    private String upiPin;

    public TransactionRequest() {};

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getAmount() {
        return amount;
    }
    public void setUpiPin(String upiPin) {this.upiPin = upiPin;}
    public String getUpiPin() {return upiPin;}
    public void setReceiverPhone(String receiverPhone) {this.receiverPhone = receiverPhone;}
    public String getReceiverPhone() {return receiverPhone;}
    public void setSenderUpiId(String senderUpiId) {this.senderUpiId = senderUpiId;}
    public String getSenderUpiId() {return senderUpiId;}
    }