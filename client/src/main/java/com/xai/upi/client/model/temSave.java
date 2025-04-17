package com.xai.upi.client.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class temSave{
    @Id
    private String id;
    private String email;
    private String bankName;
    private String accountNumber;

    public temSave(String email,String bank,String accountNumber){
        this.email=email;
        this.bankName=bank;
        this.accountNumber=accountNumber;
    }
//    public temSaveData(String email , String bankName , String accountNumber){
//        this.email = email;
//        this.bankName = bankName;
//        this.accountNumber = accountNumber;
//    }
    public String getId() {return id;}

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getBankName() {
        return bankName;
    }
    public String getEmail() {return email;}
}