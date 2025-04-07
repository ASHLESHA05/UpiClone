package com.xai.upi.client.model;

import lombok.Data;

@Data
public class TransactionRequest {
    private String senderUpiId;
    private String receiverPhone;
    private double amount;
    private String upiPin;
}