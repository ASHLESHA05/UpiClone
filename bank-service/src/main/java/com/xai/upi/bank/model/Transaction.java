package com.xai.upi.bank.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document
public class Transaction {
    @Id
    private String id; // MongoDB uses String for IDs by convention

    private String accountId; // Reference to Account document by ID
    private String type; // "CREDIT" or "DEBIT"
    private double amount; // Positive for credit, negative for debit
    private LocalDateTime timestamp;

    // Constructors
    public Transaction() {}

    public Transaction(String accountId, String type, double amount) {
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }




    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }



}