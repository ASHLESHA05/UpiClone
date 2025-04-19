package com.xai.upi.client.dto;

import java.util.List;

public class SplitBillRequestDTO {
    private String amount;
    private List<String> members;

    // Getters and Setters
    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    @Override
    public String toString() {
        return "SplitBillRequestDTO{" +
                "amount='" + amount + '\'' +
                ", members=" + members +
                '}';
    }
}