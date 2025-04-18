package com.xai.upi.bank.controller;

import com.xai.upi.bank.model.Account;
import com.xai.upi.bank.repository.AccountRepository;
import com.xai.upi.bank.services.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class BankController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    @GetMapping("/ipc/account/{id}")
    public ResponseEntity<?> getAccount(@PathVariable String id) {
        Optional<Account> accountOptional = accountRepository.findById(id);
        if (accountOptional.isPresent()) {
            return ResponseEntity.ok(accountOptional.get());
        } else {
            System.out.println("Account not found for ID (GET): " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("Account with ID [%s] not found.", id));
        }
    }

    @PostMapping("/ipc/createAccount")
    public ResponseEntity<Account> createAccount(@RequestParam String bankName, @RequestParam String userId) {
        try {
            Account account = new Account();
            account.setBankName(bankName);
            account.setAccountNumber("ACC" + System.currentTimeMillis() + "_" + userId.substring(0, Math.min(userId.length(), 4)));
            account.setBalance(1000.0);
            account.setUserId(userId);
            Account savedAccount = accountRepository.save(account);
            System.out.println("Created account: " + savedAccount.getId() + " for user: " + userId);
            return ResponseEntity.ok(savedAccount);
        } catch (Exception e) {
            System.out.println("\n\n=========== Error creating bank account ===========\n\n");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/ipc/debit")
    public ResponseEntity<String> debit(@RequestBody Map<String,String> request) {
        String bankName = request.get("bankName");
        String accountNumber = request.get("accountNumber");
        double amount = Double.parseDouble(request.get("amount"));
        String accountId = request.get("accountNumber");
        if (amount <= 0) {
            System.out.println("Attempted debit with non-positive amount: " + amount + " for account: " + accountId);
            return ResponseEntity.badRequest().body("Debit amount must be positive.");
        }


        //Find the account using the email , hence get the user id (#id use it to get account id)
        Optional<Account> accountOptional = accountRepository.findByBankNameAndAccountNumber(bankName,accountNumber);
        if (!accountOptional.isPresent()) {
            System.out.println("Account not found for ID (Debit): " + accountId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
        }

        Account account = accountOptional.get();
        if (account.getBalance() >= amount) {
            account.setBalance(account.getBalance() - amount);
            try {
                accountRepository.save(account);
                System.out.println("Debited " + amount + " from account: " + accountId + ". New balance: " + account.getBalance());
                return ResponseEntity.ok("Debited successfully. New balance: " + account.getBalance());
            } catch (Exception e) {
                System.out.println("\n\n=========== Error saving account after debit ===========\n\n");
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save debit transaction");
            }
        } else {
            System.out.println("Insufficient balance for debit. Account: " + accountId + ", Balance: " + account.getBalance() + ", Amount: " + amount);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Insufficient balance");
        }
    }

    @PostMapping("/ipc/credit")
    public ResponseEntity<String> credit(@RequestBody Map<String,String> request) {
        String bankName = request.get("bankName");
        String accountNumber = request.get("accountNumber");
        String accountId = request.get("accountNumber");
        double amount = Double.parseDouble(request.get("amount"));
        if (amount <= 0) {
            System.out.println("Attempted credit with non-positive amount: " + amount + " for account: " + accountId);
            return ResponseEntity.badRequest().body("Credit amount must be positive.");
        }
        Optional<Account> accountOptional = accountRepository.findByBankNameAndAccountNumber(bankName,accountNumber);
        if (!accountOptional.isPresent()) {
            System.out.println("Account not found for ID (Credit): " + accountId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
        }

        Account account = accountOptional.get();
        account.setBalance(account.getBalance() + amount);
        try {
            accountRepository.save(account);
            System.out.println("Credited " + amount + " to account: " + accountId + ". New balance: " + account.getBalance());
            return ResponseEntity.ok("Credited successfully. New balance: " + account.getBalance());
        } catch (Exception e) {
            System.out.println("\n\n=========== Error saving account after credit ===========\n\n");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save credit transaction");
        }
    }

    @PostMapping("/ipc/getAccdata")
    public ResponseEntity<List<Account>> getAccdata(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String bankName = request.get("bankName");
        System.out.println("In POST getAccdata");
        System.out.println(phone);
        System.out.println(bankName);
        if (phone == null || bankName == null) {
            return ResponseEntity.badRequest().build();
        }
        List<Account> accounts = accountService.findAccountsByPhoneAndBankName(phone, bankName);
        System.out.println("Accounts" + accounts);
        return ResponseEntity.ok(accounts);
    }


    @PostMapping("/ipc/getCardData")
    public ResponseEntity<List<Account>> getCardData(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String bankName = request.get("bankName");
        if (email == null || bankName == null) {
            return ResponseEntity.badRequest().build();
        }

        List<Account> cards = accountService.getCardByEmail(email, bankName);
        System.out.println("Cards" + cards);
        return ResponseEntity.ok(cards);
    }

}