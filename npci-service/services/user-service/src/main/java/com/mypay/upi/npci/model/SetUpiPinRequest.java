package com.mypay.upi.npci.model;

public class SetUpiPinRequest {
    private String userId;
    private String upiPin;
    private String otp;

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUpiPin() { return upiPin; }
    public void setUpiPin(String upiPin) { this.upiPin = upiPin; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}