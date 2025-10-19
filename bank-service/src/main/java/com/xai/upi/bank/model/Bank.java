package com.xai.upi.bank.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "banks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bank {

    @Id
    private String id; // e.g., sbi, canara

    private String fullName; // State Bank of India

    private String symbolUrl; // URL of logo
}