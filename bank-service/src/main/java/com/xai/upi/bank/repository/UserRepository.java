package com.xai.upi.bank.repository;

import com.xai.upi.bank.model.User;
import com.xai.upi.bank.repository.custom.CustomUserRepository;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String>, CustomUserRepository {
    Optional<User> findByEmailAndBank(String email, String bank);
    long countByBank(String bank);
}
