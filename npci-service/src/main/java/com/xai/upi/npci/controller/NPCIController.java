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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
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
    private static final String BANK_BASE_URL = "http://localhost:8081/api";

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        return headers;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        String phone = request.get("phone");
        String name = request.get("name");

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setName(name);
        user.setUpiId(generateUpiId(email));
        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent() && passwordEncoder.matches(password, userOptional.get().getPassword())) {
            return ResponseEntity.ok("Login successful");
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    @PostMapping("/ipc/linkBankAccount")
    public ResponseEntity<String> linkBankAccount(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String bankName = request.get("bankName");

        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("bankName", bankName, "userId", userId), getHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(
                BANK_BASE_URL + "/ipc/createAccount",
                HttpMethod.POST,
                entity,
                Map.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return ResponseEntity.ok("Bank account linked successfully");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to link bank account");
    }

    @PostMapping("/ipc/setUpiPin")
    public ResponseEntity<String> setUpiPin(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String upiPin = request.get("upiPin");

        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User user = userOptional.get();
        user.setUpiPin(passwordEncoder.encode(upiPin));
        userRepository.save(user);

        return ResponseEntity.ok("UPI PIN set successfully");
    }

    @PostMapping("/ipc/transaction")
    public ResponseEntity<String> makeTransaction(@RequestBody Map<String, String> request) {
        String fromUserId = request.get("fromUserId");
        String toUpiId = request.get("toUpiId");
        String amountStr = request.get("amount");
        String upiPin = request.get("upiPin");

        Optional<User> fromUserOptional = userRepository.findById(fromUserId);
        Optional<User> toUserOptional = userRepository.findByUpiId(toUpiId);

        if (!fromUserOptional.isPresent() || !toUserOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User fromUser = fromUserOptional.get();
        if (!passwordEncoder.matches(upiPin, fromUser.getUpiPin())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid UPI PIN");
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid amount");
        }

        // Assuming bank-service handles actual debit/credit
        HttpEntity<Map<String, String>> debitEntity = new HttpEntity<>(
                Map.of("accountId", fromUserId, "amount", amountStr),
                getHeaders()
        );
        ResponseEntity<String> debitResponse = restTemplate.exchange(
                BANK_BASE_URL + "/ipc/debit",
                HttpMethod.POST,
                debitEntity,
                String.class
        );

        if (debitResponse.getStatusCode() != HttpStatus.OK) {
            return debitResponse;
        }

        HttpEntity<Map<String, String>> creditEntity = new HttpEntity<>(
                Map.of("accountId", toUserOptional.get().getId(), "amount", amountStr),
                getHeaders()
        );
        ResponseEntity<String> creditResponse = restTemplate.exchange(
                BANK_BASE_URL + "/ipc/credit",
                HttpMethod.POST,
                creditEntity,
                String.class
        );

        if (creditResponse.getStatusCode() == HttpStatus.OK) {
            LocalDateTime time = LocalDateTime.now();
            Date date = Date.from(time.atZone(ZoneId.systemDefault()).toInstant());

            Transaction transaction = new Transaction();
            transaction.setFromUserId(fromUserId);
            transaction.setToUpiId(toUpiId);
            transaction.setAmount(amount);
            transaction.setTimestamp(date);
            transaction.setStatus("SUCCESS");
            transactionRepository.save(transaction);
            return ResponseEntity.ok("Transaction successful");
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Transaction failed");
    }

    @GetMapping("/ipc/transactions")
    public ResponseEntity<List<Transaction>> getTransactionsInternal(@RequestParam String upiId) {
        return ResponseEntity.ok(transactionRepository.findByFromUserIdOrToUpiId(upiId, upiId));
    }

    @PostMapping("/ipc/checkBalance")
    public ResponseEntity<Map<String, Object>> checkBalance(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String upiPin = request.get("upiPin");

        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        User user = userOptional.get();
        if (!passwordEncoder.matches(upiPin, user.getUpiPin())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid UPI PIN"));
        }

        HttpEntity<String> entity = new HttpEntity<>(getHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(
                BANK_BASE_URL + "/account/" + userId,
                HttpMethod.GET,
                entity,
                Map.class
        );

        return ResponseEntity.ok(response.getBody());
    }

    @GetMapping("/getAcc-phn-name")
    public ResponseEntity<List<Map>> getAccdata(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String bankName = request.get("bankName");
        if (phone == null || bankName == null) {
            return ResponseEntity.badRequest().build();
        }
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("phone", phone, "bankName", bankName), getHeaders());
        ResponseEntity<List> response = restTemplate.exchange(
                BANK_BASE_URL + "/getAccdata",
                HttpMethod.GET,
                entity,
                List.class
        );
        return ResponseEntity.ok(response.getBody());
    }

    @GetMapping("/getOtp")
    public ResponseEntity<Integer> getOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        User user = userOptional.get();
        String otp = generateOtp();
        user.setOtp(otp);
        user.setOtpGeneratedAt(LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok(Integer.parseInt(otp));
    }

    @GetMapping("/verifyOtp")
    public ResponseEntity<Boolean> verifyOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        if (email == null || otp == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        User user = userOptional.get();
        if (user.getOtpGeneratedAt().plusMinutes(5).isBefore(LocalDateTime.now())) {
            return ResponseEntity.ok(false);
        }
        boolean isValid = otp.equals(user.getOtp());
        if (isValid) {
            user.setOtp(null);
            user.setOtpGeneratedAt(null);
            userRepository.save(user);
        }
        return ResponseEntity.ok(isValid);
    }

    @GetMapping("/getCardData")
    public ResponseEntity<List<Map>> getCardData(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String bankName = request.get("bankName");
        if (email == null || bankName == null) {
            return ResponseEntity.badRequest().build();
        }
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("email", email, "bankName", bankName), getHeaders());
        ResponseEntity<List> response = restTemplate.exchange(
                BANK_BASE_URL + "/getCardData",
                HttpMethod.GET,
                entity,
                List.class
        );
        return ResponseEntity.ok(response.getBody());
    }

    @GetMapping("/verifyCard")
    public ResponseEntity<Boolean> verifyCardData(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String bankName = request.get("bankName");
        String cvv = request.get("cvv");
        String cardNumber = request.get("cardNumber");
        if (email == null || bankName == null || cvv == null || cardNumber == null) {
            return ResponseEntity.badRequest().build();
        }
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(
                Map.of("email", email, "bankName", bankName, "cvv", cvv, "cardNumber", cardNumber),
                getHeaders()
        );
        ResponseEntity<List> cardResponse = restTemplate.exchange(
                BANK_BASE_URL + "/getCardData",
                HttpMethod.GET,
                new HttpEntity<>(Map.of("email", email, "bankName", bankName), getHeaders()),
                List.class
        );
        List<Map> cards = cardResponse.getBody();
        if (cards != null) {
            for (Map card : cards) {
                if (card.get("atmCardNumber").equals(cardNumber) && card.get("cvv").equals(cvv)) {
                    return ResponseEntity.ok(true);
                }
            }
        }
        return ResponseEntity.ok(false);
    }

    @PostMapping("/saveUpiPin")
    public ResponseEntity<Boolean> saveUpiData(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String upiPin = request.get("upiPin");
        if (email == null || upiPin == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        User user = userOptional.get();
        user.setUpiPin(passwordEncoder.encode(upiPin));
        userRepository.save(user);
        return ResponseEntity.ok(true);
    }

    private String generateUpiId(String email) {
        return email.split("@")[0] + "@mypay";
    }

    private String generateOtp() {
        return String.format("%04d", (int) (Math.random() * 9000) + 1000);
    }
}