package com.xai.upi.client.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

public class CustomUserDetails implements UserDetails {

    private final String userId;
    private final String email;
    private final String password;
    private final String upiId;
    private final String bankAccountId;
    private final String loginPin;

    public CustomUserDetails(String userId, String email, String password, String upiId, String bankAccountId, String loginPin) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.upiId = upiId;
        this.bankAccountId = bankAccountId;
        this.loginPin = loginPin;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email; // Using email as username for Spring Security
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    public String getLoginPin() { // Changed to String to avoid parsing issues
        return loginPin;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public String getUserId() {
        return userId;
    }

    public String getUpiId() {
        return upiId;
    }

    public String getBankAccountId() {
        return bankAccountId;
    }

    public String getEmail() {
        return email;
    }
    public Map<String, String> getFullUserDetails() {
        Map<String, String> userDetails = new HashMap<>();
        userDetails.put("id", userId);
        userDetails.put("email", email);
        userDetails.put("upiId", upiId);
        return userDetails;
    }

}