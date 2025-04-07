package com.xai.upi.bank.repository;

import com.xai.upi.bank.model.Account;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AccountRepository extends MongoRepository<Account, String> {
}