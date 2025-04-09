package com.xai.upi.bank.repository;

import com.xai.upi.bank.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    List<Transaction> findByAccountId(String accountId);

    @Query("{ 'timestamp': { $gte: ?0, $lt: ?1 }, 'accountId': { $in: ?2 } }")
    List<Transaction> findByDateRangeAndAccounts(LocalDateTime start, LocalDateTime end, List<String> accountIds);

    @Query("{ 'type': 'DEBIT', 'accountId': { $in: ?0 } }")
    List<Transaction> findDebitsByAccountIds(List<String> accountIds);

    @Query("{ 'type': 'CREDIT', 'accountId': { $in: ?0 } }")
    List<Transaction> findCreditsByAccountIds(List<String> accountIds);

}