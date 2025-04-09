package com.xai.upi.bank.services;

import com.xai.upi.bank.model.Account;
import com.xai.upi.bank.model.Transaction;
import com.xai.upi.bank.repository.AccountRepository;
import com.xai.upi.bank.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
public class TransactionService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    public List<Transaction> findByAccountId(String accountId) {
        return transactionRepository.findByAccountId(accountId);
    }

    public List<Transaction> findByBankAndDate(String bankName, LocalDate date) {
        List<String> accountIds = accountRepository.findByBankName(bankName)
                .stream()
                .map(Account::getId)
                .collect(Collectors.toList());
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        logger.info("Fetching transactions for bank: {}, date: {}, accountIds: {}", bankName, date, accountIds);
        logger.info("Date range - Start: {}, End: {}", start, end);

        List<Transaction> transactions = transactionRepository.findByDateRangeAndAccounts(start, end, accountIds);
        logger.info("Found {} transactions", transactions.size());
        transactions.forEach(t -> logger.info("Transaction: {}", t));

        return transactions;
    }

    public double getTotalDebits(String bankName) {
        List<String> accountIds = accountRepository.findByBankName(bankName)
                .stream()
                .map(Account::getId) // Extract the 'id' field from each Account
                .collect(Collectors.toList());
        return transactionRepository.findDebitsByAccountIds(accountIds).stream()
                .mapToDouble(Transaction::getAmount).sum();
    }

    public double getTotalCredits(String bankName) {
        List<String> accountIds = accountRepository.findByBankName(bankName)
                .stream()
                .map(Account::getId) // Extract the 'id' field from each Account
                .collect(Collectors.toList());
        return transactionRepository.findCreditsByAccountIds(accountIds).stream()
                .mapToDouble(Transaction::getAmount).sum();
    }
}