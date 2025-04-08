package com.xai.upi.bank.repository;

import com.xai.upi.bank.model.Bank;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface BankRepository extends MongoRepository<Bank, String> {
    // findById is already provided by MongoRepository
    // Optional<Bank> findById(String id);
}