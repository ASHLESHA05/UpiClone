package com.xai.upi.bank.services;

import com.xai.upi.bank.model.Account;
import com.xai.upi.bank.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.List;

@Service
public class AccountService {
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public Account createAccount(String bank, String userId, double initialAmount) {
        Optional<Account> existing = accountRepository.findByUserIdAndBankName(userId, bank);
        if (existing.isPresent()) {
            throw new RuntimeException("Account already exists for this user in " + bank);
        }
        Account account = new Account();
        account.setBankName(bank);
        account.setUserId(userId);
        account.setBalance(initialAmount);
        account.setAccountNumber("ACC" + String.format("%011d", (long) (Math.random() * 1e11)));
        account.setCvv(String.format("%03d", (int) (Math.random() * 1000))); // Random 3-digit CVV
        return accountRepository.save(account);
    }

    public Account setPin(String userId, String bank, String pin) {
        Account account = accountRepository.findByUserIdAndBankName(userId, bank)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        account.setPin(passwordEncoder.encode(pin));
        return accountRepository.save(account);
    }
    public List<Account> findAllByBankName(String bank){
        List<Account> allaccounts = accountRepository.findByBankName(bank);
        return allaccounts;
    }
    public Optional<Account> findByUserIdAndBankName(String userId, String bankName) {
        Optional<Account> account = accountRepository.findByUserIdAndBankName(userId, bankName);
        return account; // Or throw exception if not found
    }

}