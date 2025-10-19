package com.xai.upi.bank.services;

import com.xai.upi.bank.model.Account;
import com.xai.upi.bank.model.Transaction;
import com.xai.upi.bank.repository.AccountRepository;
import com.xai.upi.bank.repository.TransactionRepository;
import com.xai.upi.bank.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserService userService; // Assumed to exist

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
        Transaction transaction = new Transaction(accountId, "DEBIT", amount);
        transactionRepository.save(transaction);
    }

    public Account findById(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }

    // Convert repository's Optional<Account> to List<Account> for callers that expect lists
    public List<Account> findByUserIdAndBankName(String userId, String bankName) {
        List<Account> accounts = accountRepository.findByUserIdAndBankName(userId, bankName);
        return accounts != null ? accounts : Collections.emptyList();
    }


    public Optional<Account> findFirstByUserIdAndBankName(String userId, String bankName) {
        return findByUserIdAndBankName(userId, bankName).stream().findFirst();
    }

    public void setPin(String accountNumber,String bank, String pin) {
        Account account = findByAccountNumberAndBankName(accountNumber,bank);
        account.setPin(pin); // Hash if needed, e.g., using PasswordEncoder
        accountRepository.save(account);
    }

    public void createAccount(String bankName, String userId, double initialAmount, String accountType, boolean generateAtmCard) {
        // Check if user already has an account
        if (!findByUserIdAndBankName(userId, bankName).isEmpty()) {
            throw new RuntimeException("User already has an account in this bank");
        }
        Account account = new Account();
        account.setBankName(bankName);
        account.setUserId(userId);
        account.setBalance(initialAmount);
        account.setAccountNumber(generateAccountNumber());
        account.setAccountType(accountType);
        if (generateAtmCard) {
            account.setAtmCardNumber(generateAtmCardNumber());
            account.setCvv(generateCvv());
        } else {
            account.setCvv("NA");
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
        if (account.getAtmCardNumber() == null || account.getAtmCardNumber().isEmpty()) {
            account.setAtmCardNumber(generateAtmCardNumber());
            account.setCvv(generateCvv());
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
        return String.format("%03d", random.nextInt(1000));
    }

    public List<Account> findAllByBankName(String bankName) {
        return accountRepository.findByBankName(bankName);
    }

    public List<Account> findAccountsByPhoneAndBankName(String phone, String bankName) {
        List<String> userIds = userService.findUsersId(phone);
        List<Account> result = new ArrayList<>();
        for (String userId : userIds) {
            List<Account> accs = findByUserIdAndBankName(userId, bankName);
            result.addAll(accs);
        }
        return result;
    }

    public List<Account> getCardByEmail(String email, String bankName) {
        Optional<User> res = userService.finduserByEmailBank(email, bankName);

        if (res.isPresent()) {
            String userId = res.get().getId();
            return findByUserIdAndBankName(userId, bankName);
        }

        return Collections.emptyList();
    }
    public Account findByAccountNumberAndBankName(String accountNumber, String bankName) {
        return accountRepository.findByBankNameAndAccountNumber(bankName, accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }
}