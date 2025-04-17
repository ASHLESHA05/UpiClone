package com.xai.upi.client.model;

public class SignUpRequest {

    private String name;
    private String phone;
    private String email;
    private String aadhar;
    private String password;
    private String username;
    private Integer loginPin;
    private String bankName;

    // Getters and Setters
    public String getBankName(){return bankName;}
    public void setBankName(String bankName){this.bankName = bankName;}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAadhar() {
        return aadhar;
    }

    public void setAadhar(String aadhar) {
        this.aadhar = aadhar;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getLoginPin() {
        return loginPin;
    }

    public void setLoginPin(Integer loginPin) {
        this.loginPin = loginPin;
    }
}