package com.xai.upi.npci.controller;

import com.xai.upi.npci.model.Transaction;
import com.xai.upi.npci.model.User;
import com.xai.upi.npci.repository.TransactionRepository;
import com.xai.upi.npci.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class NPCIController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RestTemplate restTemplate;

    private static final String INTERNAL_TOKEN = "uyguyfgbsvbcug76t7632$%@^@t";

    @PostMapping("/ipc/signup")
    public ResponseEntity<Map<String, String>> signUp(@RequestBody Map<String, String> request) {
        System.out.println("\n=======================================================\nOTP GENERATED\n");

        User user = new User();
        user.setName(request.get("name"));
        user.setPhone(request.get("phone"));
        user.setEmail(request.get("email"));
        user.setAadhar(request.get("aadhar"));
        user.setUpiId(request.get("phone") + "@upi");

        String otp = generateOtp();
        System.out.println("OTP for " + user.getEmail() + ": " + otp);
        user.setOtp(otp);
        user.setOtpGeneratedAt(LocalDateTime.now());

        userRepository.save(user);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "http://bank-service:8081/api/ipc/createAccount?bankName=" + request.get("bankName") + "&userId=" + user.getId(),
                HttpMethod.POST, entity, Map.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            String accountId = (String) response.getBody().get("id");
            user.setBankAccountId(accountId);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of(
                    "message", "Signup successful. Please set your UPI PIN.",
                    "userId", user.getId()
            ));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to create bank account"));
    }

    @PostMapping("/ipc/setUpiPin")
    public ResponseEntity<Map<String, String>> setUpiPin(@RequestBody Map<String, String> request) {
        Optional<User> userOptional = userRepository.findById(request.get("userId"));
        if (!userOptional.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
        }

        User user = userOptional.get();
        if (user.getOtpGeneratedAt().plusMinutes(5).isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("message", "OTP expired"));
        }
        if (!request.get("otp").equals(user.getOtp())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid OTP"));
        }
        user.setUpiPin(passwordEncoder.encode(request.get("upiPin")));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "UPI PIN set successfully"));
    }

    @PostMapping("/ipc/transaction")
    public ResponseEntity<Map<String, String>> makeTransaction(@RequestBody Map<String, Object> request) {
        Optional<User> senderOptional = userRepository.findByUpiId((String) request.get("senderUpiId"));
        if (!senderOptional.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Sender not found"));
        }
        User sender = senderOptional.get();

        User receiver = userRepository.findByPhone((String) request.get("receiverPhone")).orElse(null);

        if (!passwordEncoder.matches((String) request.get("upiPin"), sender.getUpiPin())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid UPI PIN"));
        }

        Transaction transaction = new Transaction();
        transaction.setSenderUpiId(sender.getUpiId());
        transaction.setAmount(((Number) request.get("amount")).doubleValue());
        transaction.setStatus("PENDING");
        transaction.setTimestamp(new Date());
        transactionRepository.save(transaction);

        if (receiver == null) {
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);
            return ResponseEntity.badRequest().body(Map.of("message", "Receiver not found. Amount refunded."));
        }

        transaction.setReceiverUpiId(receiver.getUpiId());

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> balanceResponse = restTemplate.getForEntity(
                "http://bank-service:8081/api/account/" + sender.getBankAccountId(), Map.class);

        if (balanceResponse.getStatusCode() != HttpStatus.OK || balanceResponse.getBody() == null) {
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);
            return ResponseEntity.badRequest().body(Map.of("message", "Unable to fetch sender balance"));
        }

        Double balance = (Double) balanceResponse.getBody().get("balance");
        double amount = ((Number) request.get("amount")).doubleValue();
        if (balance < amount) {
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);
            return ResponseEntity.badRequest().body(Map.of("message", "Insufficient balance"));
        }

        ResponseEntity<String> debitResponse = restTemplate.exchange(
                "http://bank-service:8081/api/ipc/debit?accountId=" + sender.getBankAccountId() + "&amount=" + amount,
                HttpMethod.POST, entity, String.class);
        if (debitResponse.getStatusCode() != HttpStatus.OK) {
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);
            return ResponseEntity.badRequest().body(Map.of("message", "Debit failed"));
        }

        ResponseEntity<String> creditResponse = restTemplate.exchange(
                "http://bank-service:8081/api/ipc/credit?accountId=" + receiver.getBankAccountId() + "&amount=" + amount,
                HttpMethod.POST, entity, String.class);
        if (creditResponse.getStatusCode() != HttpStatus.OK) {
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);
            return ResponseEntity.badRequest().body(Map.of("message", "Credit failed"));
        }

        transaction.setStatus("SUCCESS");
        transactionRepository.save(transaction);
        return ResponseEntity.ok(Map.of("message", "Transaction successful"));
    }

    @GetMapping("/ipc/user")
    public ResponseEntity<?> getUserInternal(@RequestParam String upiId) {
        Optional<User> userOptional = userRepository.findByUpiId(upiId);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found with UPI ID: " + upiId));
        }

        User user = userOptional.get();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> accountResponse = restTemplate.exchange(
                "http://bank-service:8081/api/account/" + user.getBankAccountId(),
                HttpMethod.GET, entity, Map.class);

        Map<String, Object> userData = Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "phone", user.getPhone(),
                "email", user.getEmail(),
                "upiId", user.getUpiId(),
                "balance", accountResponse.getBody().get("balance")
        );
        return ResponseEntity.ok(userData);
    }

    @GetMapping("/ipc/transactions")
    public ResponseEntity<List<Transaction>> getTransactionsInternal(@RequestParam String upiId) {
        return ResponseEntity.ok(transactionRepository.findBySenderUpiIdOrReceiverUpiId(upiId, upiId));
    }

    @PostMapping("/changeUpiPin")
    public ResponseEntity<String> changeUpiPin(@RequestBody Map<String, String> request) {
        Optional<User> userOptional = userRepository.findById(request.get("userId"));
        if (!userOptional.isPresent()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = userOptional.get();
        String otp = generateOtp();
        System.out.println("OTP for " + user.getEmail() + ": " + otp);
        if (request.get("otp").equals(otp)) {
            user.setUpiPin(passwordEncoder.encode(request.get("upiPin")));
            userRepository.save(user);
            return ResponseEntity.ok("UPI PIN changed successfully");
        }
        return ResponseEntity.badRequest().body("Invalid OTP");
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUser(@RequestParam String upiId) {
        Optional<User> userOptional = userRepository.findByUpiId(upiId);
        if (!userOptional.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userOptional.get());
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getTransactions(@RequestParam String upiId) {
        return ResponseEntity.ok(transactionRepository.findBySenderUpiIdOrReceiverUpiId(upiId, upiId));
    }

    private String generateOtp() {
        return String.valueOf((int) (Math.random() * 9000) + 1000);
    }
}