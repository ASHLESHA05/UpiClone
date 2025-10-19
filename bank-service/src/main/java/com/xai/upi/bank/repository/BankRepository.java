package com.xai.upi.bank.repository;

import com.xai.upi.bank.model.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BankRepository extends JpaRepository<Bank, String> {
    // findById is already provided by JpaRepository
    // Optional<Bank> findById(String id);
}