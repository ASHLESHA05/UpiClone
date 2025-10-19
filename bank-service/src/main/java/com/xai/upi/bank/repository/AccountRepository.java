package com.xai.upi.bank.repository;

import com.xai.upi.bank.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {
    @Query("SELECT a FROM Account a WHERE a.userId = :userId AND a.bankName = :bankName")
    List<Account> findByUserIdAndBankName(String userId, String bankName);

    List<Account> findByBankName(String bankName);

    Optional<Account> findByBankNameAndAccountNumber(String bankName, String accountNumber);
}