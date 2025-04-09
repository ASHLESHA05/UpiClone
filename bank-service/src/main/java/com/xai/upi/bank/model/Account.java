package com.xai.upi.bank.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class Account {
    @Id
    private String id;
    private String bankName;
    private String accountNumber;

    private String accountType;
    private String atmCardNumber;

    private double balance;
    private String userId;
    private String pin; // Hashed
    private String cvv;

    // Constructors, getters, setters
    public Account() {}

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
    public String getAtmCardNumber() {return atmCardNumber;}
    public void setAtmCardNumber(String atmCardNumber) {this.atmCardNumber = atmCardNumber;}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPin() { return pin; }
    public void setPin(String pin) { this.pin = pin; }
    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
}