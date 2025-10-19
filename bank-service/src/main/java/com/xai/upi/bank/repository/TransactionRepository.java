package com.xai.upi.bank.repository;

import com.xai.upi.bank.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    @Query("SELECT t FROM Transaction t WHERE t.accountId = :accountId")
    List<Transaction> findByAccountId(String accountId);

    @Query("SELECT t FROM Transaction t WHERE t.timestamp >= :start AND t.timestamp < :end AND t.accountId IN :accountIds")
    List<Transaction> findByDateRangeAndAccounts(LocalDateTime start, LocalDateTime end, List<String> accountIds);

    @Query("SELECT t FROM Transaction t WHERE t.type = 'DEBIT' AND t.accountId IN :accountIds")
    List<Transaction> findDebitsByAccountIds(List<String> accountIds);

    @Query("SELECT t FROM Transaction t WHERE t.type = 'CREDIT' AND t.accountId IN :accountIds")
    List<Transaction> findCreditsByAccountIds(List<String> accountIds);
}