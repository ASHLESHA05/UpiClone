package com.xai.upi.bank.repository.custom;

import java.util.List;

public interface CustomUserRepository {
    List<String> findUserIdsByPhone(String phone);
}