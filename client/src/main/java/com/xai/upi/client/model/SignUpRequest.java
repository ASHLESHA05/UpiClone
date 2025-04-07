package com.xai.upi.client.model;

import lombok.Data;

@Data
public class SignUpRequest {
    private String name;
    private String phone;
    private String email;
    private String aadhar;
    private String bankName;
}