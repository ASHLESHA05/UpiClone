package com.xai.upi.bank.controller;

import com.xai.upi.bank.model.Account;
import com.xai.upi.bank.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BankController {

    @Autowired
    private AccountRepository accountRepository;

    // Public endpoint (optional, accessible if needed by Thymeleaf or external clients)
    @GetMapping("/account/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable String id) {
        Account account = accountRepository.findById(id).orElseThrow(() -> new RuntimeException("Account not found"));
        return ResponseEntity.ok(account);
    }

    // IPC endpoints (internal use only)
    @PostMapping("/ipc/createAccount")
    public ResponseEntity<Account> createAccount(@RequestParam String bankName, @RequestParam String userId) {
        Account account = new Account();
        account.setBankName(bankName);
        account.setAccountNumber("ACC" + System.currentTimeMillis());
        account.setBalance(1000.0);
        account.setUserId(userId);
        accountRepository.save(account);
        return ResponseEntity.ok(account);
    }

    @PostMapping("/ipc/debit")
    public ResponseEntity<String> debit(@RequestParam String accountId, @RequestParam double amount) {
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Account not found"));
        if (account.getBalance() >= amount) {
            account.setBalance(account.getBalance() - amount);
            accountRepository.save(account);
            return ResponseEntity.ok("Debited successfully");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Insufficient balance");
    }

    @PostMapping("/ipc/credit")
    public ResponseEntity<String> credit(@RequestParam String accountId, @RequestParam double amount) {
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new RuntimeException("Account not found"));
        account.setBalance(account.getBalance() + amount);
        accountRepository.save(account);
        return ResponseEntity.ok("Credited successfully");
    }
}