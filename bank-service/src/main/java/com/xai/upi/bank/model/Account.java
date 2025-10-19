package com.xai.upi.bank.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(generator = "uuid2")
    @org.hibernate.annotations.GenericGenerator(name = "uuid2", strategy = "uuid2")
    private String id;

    private String bankName;

    private String userId;

    @Column(unique = true, nullable = false)
    private String accountNumber;

    private String accountType;

    private String atmCardNumber;

    private double balance;

    private String pin; // hashed

    private String cvv;
}