package com.xai.upi.npci.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String name;
    private String phone;
    private String email;
    private String aadhar;
    private String upiId;
    private String upiPin;
    private String bankAccountId;
    private String bankName;
    private String otp;
    private LocalDateTime otpGeneratedAt;
    private String password;
    private List<String> friends;
    private List<String> familyMembers;
    private String userId;

    public String getBankName(){return bankName;}
    public void setBankName(String bankName){this.bankName = bankName;}
    public String getUserId(){ return userId; }
    public void setUserId(String userId){ this.userId = userId; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAadhar() { return aadhar; }
    public void setAadhar(String aadhar) { this.aadhar = aadhar; }
    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }
    public String getUpiPin() { return upiPin; }
    public void setUpiPin(String upiPin) { this.upiPin = upiPin; }
    public String getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(String bankAccountId) { this.bankAccountId = bankAccountId; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
    public LocalDateTime getOtpGeneratedAt() { return otpGeneratedAt; }
    public void setOtpGeneratedAt(LocalDateTime otpGeneratedAt) { this.otpGeneratedAt = otpGeneratedAt; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public List<String> getFriends() { return friends; }
    public void setFriends(List<String> friends) { this.friends = friends; }
    public List<String> getFamilyMembers() { return familyMembers; }
    public void setFamilyMembers(List<String> familyMembers) { this.familyMembers = familyMembers; }
}