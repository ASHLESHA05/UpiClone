package com.xai.upi.bank.services;

import com.xai.upi.bank.model.Account;
import com.xai.upi.bank.model.Transaction;
import com.xai.upi.bank.repository.AccountRepository;
import com.xai.upi.bank.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;
import java.util.List;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Transactional
    public void creditAccount(String accountId, double amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        account.setBalance(account.getBalance() + amount);
        accountRepository.save(account);
        Transaction transaction = new Transaction(accountId, "CREDIT", amount);
        transactionRepository.save(transaction);
    }

    @Transactional
    public void debitAccount(String accountId, double amount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        if (account.getBalance() < amount) {
            throw new RuntimeException("Insufficient balance");
        }
        account.setBalance(account.getBalance() - amount);
        accountRepository.save(account);
        Transaction transaction = new Transaction(accountId, "DEBIT", -amount);
        transactionRepository.save(transaction);
    }

    public Account findById(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    public Optional<Account> findByUserIdAndBankName(String userId, String bankName) {
        return accountRepository.findByUserIdAndBankName(userId, bankName);
    }

    public void setPin(String userId, String bankName, String pin) {
        Account account = findByUserIdAndBankName(userId, bankName)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        account.setPin(pin);
        accountRepository.save(account);
    }

    public void createAccount(String bankName, String userId, double initialAmount, String accountType, boolean generateAtmCard) {
        Account account = new Account();
        account.setBankName(bankName);
        account.setUserId(userId);
        account.setBalance(initialAmount);
        account.setAccountNumber(generateAccountNumber());
        account.setAccountType(accountType);
        if (generateAtmCard) {
            account.setAtmCardNumber(generateAtmCardNumber());
            account.setCvv(generateCvv()); // Random CVV when card is generated
        } else {
            account.setCvv("NA"); // Default to NA if no card
        }
        accountRepository.save(account);
        System.out.println("\n\n==============Account created===============\n\n");
        System.out.println(initialAmount);
        if (initialAmount > 0) {
            Transaction transaction = new Transaction(account.getId(), "CREDIT", initialAmount);
            transactionRepository.save(transaction);
        }
    }

    public void generateAtmCard(String accountId) {
        Account account = findById(accountId);
        if (account.getAtmCardNumber() == null) {
            account.setAtmCardNumber(generateAtmCardNumber());
            account.setCvv(generateCvv()); // Random CVV when card is generated
            accountRepository.save(account);
        }
    }

    private String generateAccountNumber() {
        return "ACC" + System.currentTimeMillis();
    }

    private String generateAtmCardNumber() {
        Random random = new Random();
        return String.format("%04d-%04d-%04d-%04d",
                random.nextInt(10000), random.nextInt(10000),
                random.nextInt(10000), random.nextInt(10000));
    }

    private String generateCvv() {
        Random random = new Random();
        return String.format("%03d", random.nextInt(1000)); // Generates a 3-digit CVV (000-999)
    }

    // For admin: fetch all accounts by bank name (assumed method)
    public List<Account> findAllByBankName(String bankName) {
        return accountRepository.findByBankName(bankName);
    }
}