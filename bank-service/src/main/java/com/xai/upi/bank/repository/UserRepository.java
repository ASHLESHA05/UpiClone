package com.xai.upi.bank.repository;

import com.xai.upi.bank.model.User;
import com.xai.upi.bank.repository.custom.CustomUserRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String>, CustomUserRepository {
    Optional<User> findByEmailAndBank(String email, String bank);
    Optional<User> findByEmail(String email);
    long countByBank(String bank);
}