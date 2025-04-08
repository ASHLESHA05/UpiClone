package com.xai.upi.bank.repository;

import com.xai.upi.bank.model.Account;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
import java.util.List;

public interface AccountRepository extends MongoRepository<Account, String> {
    Optional<Account> findByUserIdAndBankName(String userId, String bankName);
    List<Account> findByBankName(String bankName);
}