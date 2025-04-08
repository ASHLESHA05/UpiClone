package com.xai.upi.bank.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "banks")
public class Bank {
    @Id
    private String id; // e.g., "sbi", "canara" - used in URL path
    private String fullName; // e.g., "State Bank of India"
    private String symbolUrl; // URL to the bank's logo
}