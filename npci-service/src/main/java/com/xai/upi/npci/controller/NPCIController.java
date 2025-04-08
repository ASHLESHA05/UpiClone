package com.xai.upi.npci.controller;

import com.xai.upi.npci.model.Transaction;
import com.xai.upi.npci.model.User;
import com.xai.upi.npci.repository.TransactionRepository;
import com.xai.upi.npci.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException; // Import RestClientException
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
        try {
            String phone = request.get("phone");
            String email = request.get("email");

            // Check for existing user by phone
            try {
                if (!userRepository.findByPhone(phone).isEmpty()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Phone number already registered"));
                }
            } catch (Exception e) {
                System.out.println("\n\n=========== Error checking phone existence ===========\n\n");
                e.printStackTrace(); // Print stack trace for detailed debugging
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error checking phone number"));
            }

            // Check for existing user by email
            try {
                if (!userRepository.findByEmail(email).isEmpty()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email already registered"));
                }
            } catch (Exception e) {
                System.out.println("\n\n=========== Error checking email existence ===========\n\n");
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error checking email"));
            }

            User user = new User();
            user.setName(request.get("name"));
            user.setPhone(phone);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(request.get("password")));
            user.setAadhar(request.get("aadhar"));

            // Save the new user
            try {
                userRepository.save(user);
                return ResponseEntity.ok(Map.of("message", "Signup successful. Please login.", "userId", user.getId()));
            } catch (Exception e) {
                System.out.println("\n\n=========== Error saving new user during signup ===========\n\n");
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Signup failed due to server error"));
            }

        } catch (Exception e) { // Catch unexpected errors during request processing
            System.out.println("\n\n=========== Unexpected error in signup process ===========\n\n");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "An unexpected error occurred during signup"));
        }
    }

    @PostMapping("/ipc/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        try {
            Optional<User> userOptional = userRepository.findByEmail(request.get("email"));

            if (userOptional.isPresent() && passwordEncoder.matches(request.get("password"), userOptional.get().getPassword())) {
                User user = userOptional.get();
                Map<String, Object> response = Map.of(
                        "userId", user.getId(),
                        "name", user.getName(),
                        "phone", user.getPhone(),
                        "email", user.getEmail(),
                        "upiId", user.getUpiId() != null ? user.getUpiId() : "",
                        "bankAccountId", user.getBankAccountId() != null ? user.getBankAccountId() : ""
                );
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid email or password"));
            }
        } catch (Exception e) {
            System.out.println("\n\n=========== Error during login process ===========\n\n");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "An error occurred during login"));
        }
    }

    @PostMapping("/ipc/linkBankAccount")
    public ResponseEntity<Map<String, String>> linkBankAccount(@RequestBody Map<String, String> request) {
        Optional<User> userOptional;
        User user;

        try {
            userOptional = userRepository.findById(request.get("userId"));
            if (!userOptional.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
            }
            user = userOptional.get();
        } catch (Exception e) {
            System.out.println("\n\n=========== Error finding user for linking bank account ===========\n\n");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error finding user"));
        }

        try {
            if (user.getBankAccountId() != null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Bank account already linked"));
            }

            String atmNumber = request.get("atmNumber");
            String cvv = request.get("cvv");
            if (atmNumber == null || cvv == null || !atmNumber.matches("\\d{16}") || !cvv.matches("\\d{3}")) {
                System.out.println("\n\n=========== Invalid ATM or CVV format ===========\n\n");
                System.out.println("ATM: " + atmNumber + ", CVV: " + cvv);
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid ATM number or CVV"));
            }


            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Token", INTERNAL_TOKEN);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            String bankServiceUrl = "http://bank-service:8081/api/ipc/createAccount?bankName=" + request.get("bankName") + "&userId=" + user.getId();

            ResponseEntity<Map> response;
            try {
                response = restTemplate.exchange(bankServiceUrl, HttpMethod.POST, entity, Map.class);
            } catch (RestClientException e) {
                System.out.println("\n\n=========== Error calling bank service to create account ===========\n\n");
                System.out.println("URL: " + bankServiceUrl);
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "Failed to communicate with bank service"));
            }

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String accountId = (String) response.getBody().get("id");
                if(accountId == null) {
                    System.out.println("\n\n=========== Bank service returned OK but no account ID ===========\n\n");
                    System.out.println("Response Body: " + response.getBody());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Bank service response missing account ID"));
                }

                user.setBankAccountId(accountId);
                user.setUpiId(user.getPhone() + "@" + request.get("bankName"));
                user.setOtp(generateOtp());
                user.setOtpGeneratedAt(LocalDateTime.now());

                try {
                    userRepository.save(user);
                    System.out.println("OTP for " + user.getEmail() + ": " + user.getOtp()); // Keep OTP log for debugging
                    return ResponseEntity.ok(Map.of("message", "Bank account linked. Set your UPI PIN."));
                } catch (Exception e) {
                    System.out.println("\n\n=========== Error saving user after linking bank account ===========\n\n");
                    e.printStackTrace();
                    // Consider rolling back bank account creation if possible/necessary
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to update user details after linking account"));
                }
            } else {
                System.out.println("\n\n=========== Bank service returned non-OK status or empty body ===========\n\n");
                System.out.println("Status Code: " + response.getStatusCode());
                System.out.println("Response Body: " + response.getBody());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to link bank account due to bank service error"));
            }
        } catch (Exception e) { // Catch unexpected errors during the linking process
            System.out.println("\n\n=========== Unexpected error during linkBankAccount process ===========\n\n");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "An unexpected error occurred while linking bank account"));
        }
    }


    @PostMapping("/ipc/setUpiPin")
    public ResponseEntity<Map<String, String>> setUpiPin(@RequestBody Map<String, String> request) {
        Optional<User> userOptional;
        User user;

        try {
            userOptional = userRepository.findById(request.get("userId"));
            if (!userOptional.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
            }
            user = userOptional.get();
        } catch (Exception e) {
            System.out.println("\n\n=========== Error finding user for setting UPI PIN ===========\n\n");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error finding user"));
        }

        try {
            if (user.getBankAccountId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Please link bank account first"));
            }
            if (user.getOtpGeneratedAt() == null) {
                System.out.println("\n\n=========== OTP generation time missing for user " + user.getId() + " ===========\n\n");
                return ResponseEntity.badRequest().body(Map.of("message", "OTP details missing, please try linking account again"));
            }
            if (user.getOtpGeneratedAt().plusMinutes(5).isBefore(LocalDateTime.now())) {
                return ResponseEntity.badRequest().body(Map.of("message", "OTP expired"));
            }
            if (user.getOtp() == null || !request.get("otp").equals(user.getOtp())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid OTP"));
            }
            if (request.get("upiPin") == null || request.get("upiPin").isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "UPI PIN cannot be empty"));
            }


            user.setUpiPin(passwordEncoder.encode(request.get("upiPin")));
            // Optionally clear OTP after successful use
            // user.setOtp(null);
            // user.setOtpGeneratedAt(null);

            try {
                userRepository.save(user);
                return ResponseEntity.ok(Map.of("message", "UPI PIN set successfully"));
            } catch (Exception e) {
                System.out.println("\n\n=========== Error saving user after setting UPI PIN ===========\n\n");
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to save UPI PIN"));
            }
        } catch (Exception e) { // Catch unexpected errors during PIN setting
            System.out.println("\n\n=========== Unexpected error during setUpiPin process ===========\n\n");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "An unexpected error occurred while setting UPI PIN"));
        }
    }

    @PostMapping("/ipc/transaction")
    public ResponseEntity<Map<String, String>> makeTransaction(@RequestBody Map<String, Object> request) {
        System.out.println("\n\n======== Making transaction ========\n\n");
        Transaction transaction = new Transaction(); // Create transaction object early for logging
        User sender = null;
        User receiver = null;
        String senderUpiId = null;
        String receiverPhone = null;
        Double amount = null;
        String upiPin = null;

        try {
            // --- 1. Extract and Validate Input ---
            senderUpiId = (String) request.get("senderUpiId");
            receiverPhone = (String) request.get("receiverPhone");
            upiPin = (String) request.get("upiPin");
            Object amountObj = request.get("amount");

            if (senderUpiId == null || receiverPhone == null || upiPin == null || amountObj == null) {
                System.out.println("\n\n=========== Missing parameters in transaction request ===========\n\n");
                return ResponseEntity.badRequest().body(Map.of("message", "Missing required fields: senderUpiId, receiverPhone, upiPin, amount"));
            }
            try {
                amount = ((Number) amountObj).doubleValue();
                if (amount <= 0) {
                    System.out.println("\n\n=========== Invalid transaction amount: " + amount + " ===========\n\n");
                    return ResponseEntity.badRequest().body(Map.of("message", "Transaction amount must be positive"));
                }
            } catch (ClassCastException | NullPointerException e) {
                System.out.println("\n\n=========== Invalid amount format in transaction request ===========\n\n");
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid amount format"));
            }


            // --- 2. Find Sender ---
            try {
                Optional<User> senderOptional = userRepository.findByUpiId(senderUpiId);
                if (!senderOptional.isPresent()) {
                    System.out.println("Sender not found for UPI ID: " + senderUpiId);
                    return ResponseEntity.badRequest().body(Map.of("message", "Sender not found"));
                }
                sender = senderOptional.get();
                transaction.setSenderUpiId(sender.getUpiId()); // Set early for potential failure logging
            } catch (Exception e) {
                System.out.println("\n\n=========== Error finding sender by UPI ID ===========\n\n");
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error retrieving sender details"));
            }

            // --- 3. Check Sender's Bank Account and UPI PIN setup ---
            if (sender.getBankAccountId() == null || sender.getUpiPin() == null) {
                System.out.println("\n\n=========== Sender "+ sender.getId() +" has not linked bank or set PIN ===========\n\n");
                return ResponseEntity.badRequest().body(Map.of("message", "Sender must link bank account and set UPI PIN first"));
            }


            // --- 4. Authenticate Sender ---
            try {
                if (!passwordEncoder.matches(upiPin, sender.getUpiPin())) {
                    System.out.println("Invalid upiPin for sender: " + sender.getId());
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid UPI PIN"));
                }
            } catch (Exception e) {
                System.out.println("\n\n=========== Error matching UPI PIN ===========\n\n");
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error verifying UPI PIN"));
            }

            // --- 5. Find Receiver ---
            try {
                Optional<User> receiverOptional = userRepository.findByPhone(receiverPhone);
                if (!receiverOptional.isPresent()) {
                    System.out.println("Receiver not found for phone: " + receiverPhone);
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Receiver user not found"));
                }
                receiver = receiverOptional.get();
                transaction.setReceiverUpiId(receiver.getUpiId()); // Set receiver UPI ID
                // --- 5a. Check Receiver's Bank Account ---
                if (receiver.getBankAccountId() == null) {
                    System.out.println("\n\n=========== Receiver "+ receiver.getId() +" has not linked bank account ===========\n\n");
                    return ResponseEntity.badRequest().body(Map.of("message", "Receiver has not linked a bank account"));
                }

            } catch (Exception e) {
                System.out.println("\n\n=========== Error finding receiver by phone ===========\n\n");
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error retrieving receiver details"));
            }


            // --- 6. Initialize and Save Pending Transaction ---
            transaction.setAmount(amount);
            transaction.setStatus("PENDING");
            transaction.setTimestamp(new Date());
            try {
                transactionRepository.save(transaction);
            } catch (Exception e) {
                System.out.println("\n\n=========== Error saving initial PENDING transaction ===========\n\n");
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to initiate transaction record"));
            }


            // --- 7. Check Sender Balance ---
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Token", INTERNAL_TOKEN);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            String balanceUrl = "http://bank-service:8081/api/account/" + sender.getBankAccountId();
            Double balance = 0.0;
            try {
                ResponseEntity<Map> balanceResponse = restTemplate.getForEntity(balanceUrl, Map.class);
                if (balanceResponse.getBody() == null || balanceResponse.getBody().get("balance") == null) {
                    System.out.println("\n\n=========== Bank service returned invalid balance response ===========\n\n");
                    System.out.println("URL: " + balanceUrl);
                    System.out.println("Response: " + balanceResponse);
                    transaction.setStatus("FAILED");
                    transactionRepository.save(transaction); // Update status
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to retrieve sender balance"));
                }
                balance = ((Number) balanceResponse.getBody().get("balance")).doubleValue();
                if (balance < amount) {
                    System.out.println("Insufficient balance for sender: " + sender.getId() + " (Balance: " + balance + ", Amount: " + amount + ")");
                    transaction.setStatus("FAILED");
                    transactionRepository.save(transaction);
                    return ResponseEntity.badRequest().body(Map.of("message", "Insufficient balance"));
                }
            } catch (RestClientException e) {
                System.out.println("\n\n=========== Error calling bank service for balance check ===========\n\n");
                System.out.println("URL: " + balanceUrl);
                e.printStackTrace();
                transaction.setStatus("FAILED");
                transactionRepository.save(transaction);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "Failed to check balance with bank service"));
            } catch (Exception e) { // Catch potential ClassCastException or NullPointerException
                System.out.println("\n\n=========== Error processing balance response ===========\n\n");
                System.out.println("URL: " + balanceUrl);
                e.printStackTrace();
                transaction.setStatus("FAILED");
                transactionRepository.save(transaction); // Update status
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error processing balance information"));
            }


            // --- 8. Debit Sender ---
            String debitUrl = "http://bank-service:8081/api/ipc/debit?accountId=" + sender.getBankAccountId() + "&amount=" + amount;
            try {
                ResponseEntity<String> debitResponse = restTemplate.exchange(debitUrl, HttpMethod.POST, entity, String.class);
                if (debitResponse.getStatusCode() != HttpStatus.OK) {
                    // Debit failed, transaction fails
                    System.out.println("Debit failed for sender: " + sender.getId() + " Status: " + debitResponse.getStatusCode());
                    transaction.setStatus("FAILED");
                    transactionRepository.save(transaction);
                    return ResponseEntity.badRequest().body(Map.of("message", "Debit failed at bank"));
                }
            } catch (RestClientException e) {
                System.out.println("\n\n=========== Error calling bank service for debit ===========\n\n");
                System.out.println("URL: " + debitUrl);
                e.printStackTrace();
                transaction.setStatus("FAILED");
                transactionRepository.save(transaction);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "Failed to debit sender account with bank service"));
            }

            // --- 9. Credit Receiver ---
            String creditUrl = "http://bank-service:8081/api/ipc/credit?accountId=" + receiver.getBankAccountId() + "&amount=" + amount;
            try {
                ResponseEntity<String> creditResponse = restTemplate.exchange(creditUrl, HttpMethod.POST, entity, String.class);
                if (creditResponse.getStatusCode() != HttpStatus.OK) {
                    // Credit failed, transaction fails. IMPORTANT: Need to handle potential rollback/reversal of debit.
                    // For now, just mark as failed. A more robust solution would involve a compensating transaction.
                    System.out.println("Credit failed for receiver: " + receiver.getId() + " Status: " + creditResponse.getStatusCode());
                    transaction.setStatus("FAILED");
                    // Add a note about the state? E.g., "FAILED_AFTER_DEBIT"
                    transactionRepository.save(transaction);
                    // Attempt to refund sender? This adds complexity.
                    System.out.println("\n\n=========== CRITICAL: Credit failed after successful debit! Manual intervention may be needed. ===========\n\n");
                    System.out.println("Sender: " + sender.getUpiId() + ", Receiver: " + receiver.getUpiId() + ", Amount: " + amount);
                    return ResponseEntity.badRequest().body(Map.of("message", "Credit failed at bank after debit"));
                }
            } catch (RestClientException e) {
                System.out.println("\n\n=========== Error calling bank service for credit ===========\n\n");
                System.out.println("URL: " + creditUrl);
                e.printStackTrace();
                transaction.setStatus("FAILED");
                // Add a note about the state? E.g., "FAILED_AFTER_DEBIT"
                transactionRepository.save(transaction);
                // Attempt to refund sender?
                System.out.println("\n\n=========== CRITICAL: Credit failed via exception after successful debit! Manual intervention may be needed. ===========\n\n");
                System.out.println("Sender: " + sender.getUpiId() + ", Receiver: " + receiver.getUpiId() + ", Amount: " + amount);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "Failed to credit receiver account with bank service"));
            }


            // --- 10. Finalize Successful Transaction ---
            transaction.setStatus("SUCCESS");
            try {
                transactionRepository.save(transaction);
                System.out.println("\n\n======== Transaction successful ========\n\n");
                return ResponseEntity.ok(Map.of("message", "Transaction successful"));
            } catch (Exception e) {
                System.out.println("\n\n=========== Error saving final SUCCESS transaction status ===========\n\n");
                e.printStackTrace();
                // Transaction likely completed at bank level, but internal record failed update.
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Transaction likely successful, but failed to update final status"));
            }

        } catch (Exception e) { // Catch any other unexpected errors
            System.out.println("\n\n=========== Unexpected error during makeTransaction process ===========\n\n");
            e.printStackTrace();
            // Try to mark transaction as failed if it was initialized
            if (transaction != null && transaction.getId() != null && !"SUCCESS".equals(transaction.getStatus())) {
                try {
                    transaction.setStatus("FAILED");
                    transactionRepository.save(transaction);
                } catch (Exception logEx) {
                    System.out.println("\n\n=========== Also failed to mark transaction as FAILED during error handling ===========\n\n");
                    logEx.printStackTrace();
                }
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "An unexpected error occurred during the transaction"));
        }
    }

    @GetMapping("/ipc/user")
    public ResponseEntity<?> getUserInternal(@RequestParam(required = false) String upiId, @RequestParam(required = false) String userId) {
        try {
            Optional<User> userOptional;
            if (upiId != null) {
                userOptional = userRepository.findByUpiId(upiId);
            } else if (userId != null) {
                userOptional = userRepository.findById(userId);
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "Provide upiId or userId"));
            }

            if (!userOptional.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
            }
            User user = userOptional.get();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Token", INTERNAL_TOKEN);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            Double balance = 0.0;

            if (user.getBankAccountId() != null) {
                String accountUrl = "http://bank-service:8081/api/account/" + user.getBankAccountId();
                try {
                    ResponseEntity<Map> accountResponse = restTemplate.exchange(
                            accountUrl,
                            HttpMethod.GET, entity, Map.class);

                    if (accountResponse.getStatusCode() == HttpStatus.OK && accountResponse.getBody() != null && accountResponse.getBody().get("balance") != null) {
                        try {
                            balance = ((Number) accountResponse.getBody().get("balance")).doubleValue();
                        } catch (ClassCastException | NullPointerException e) {
                            System.out.println("\n\n=========== Error casting balance from bank service response ===========\n\n");
                            System.out.println("User: " + user.getId() + ", Account: " + user.getBankAccountId());
                            System.out.println("Response Body: " + accountResponse.getBody());
                            e.printStackTrace();
                            // Return user data without balance or with zero balance? Or return error?
                            // Let's return 0.0 for now, but log the error.
                            balance = 0.0; // Set default/error value
                        }
                    } else {
                        System.out.println("\n\n=========== Non-OK response or missing balance from bank service for user " + user.getId() + " ===========\n\n");
                        System.out.println("URL: " + accountUrl);
                        System.out.println("Status Code: " + accountResponse.getStatusCode());
                        System.out.println("Response Body: " + accountResponse.getBody());
                        // Decide how to handle this - return 0 balance or error?
                        balance = 0.0; // Set default/error value
                    }
                } catch (RestClientException e) {
                    System.out.println("\n\n=========== Error calling bank service to get account details/balance ===========\n\n");
                    System.out.println("URL: " + accountUrl);
                    e.printStackTrace();
                    // Decide how to handle this - return 0 balance or error?
                    balance = 0.0; // Set default/error value
                    // Optionally return an error response instead:
                    // return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "Could not retrieve balance from bank service"));
                }
            }

            Map<String, Object> userData = Map.of(
                    "id", user.getId(),
                    "name", user.getName(),
                    "phone", user.getPhone(),
                    "email", user.getEmail(),
                    "upiId", user.getUpiId() != null ? user.getUpiId() : "",
                    "bankAccountId", user.getBankAccountId() != null ? user.getBankAccountId() : "",
                    "balance", balance // Return balance (potentially 0.0 if retrieval failed)
            );
            return ResponseEntity.ok(userData);

        } catch (Exception e) {
            System.out.println("\n\n=========== Unexpected error in getUserInternal ===========\n\n");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "An unexpected error occurred retrieving user data"));
        }
    }

    @GetMapping("/ipc/transactions")
    public ResponseEntity<List<Transaction>> getTransactionsInternal(@RequestParam String upiId) {
        try {
            if (upiId == null || upiId.isEmpty()) {
                // Returning bad request instead of empty list if upiId is missing
                return ResponseEntity.badRequest().body(null); // Or Map.of("message", "upiId is required")
            }
            List<Transaction> transactions = transactionRepository.findBySenderUpiIdOrReceiverUpiId(upiId, upiId);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            System.out.println("\n\n=========== Error retrieving transactions for UPI ID: " + upiId + " ===========\n\n");
            e.printStackTrace();
            // Return internal server error instead of empty list on failure
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // Or Map.of("message", "Error retrieving transactions")
        }
    }

    @PostMapping("/ipc/checkBalance")
    public ResponseEntity<Map<String, Object>> checkBalance(@RequestBody Map<String, String> request) {
        Optional<User> userOptional;
        User user;
        String userId = request.get("userId");
        String upiPin = request.get("upiPin");

        if(userId == null || upiPin == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "userId and upiPin are required"));
        }

        try {
            userOptional = userRepository.findById(userId);
            if (!userOptional.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("message", "User not found"));
            }
            user = userOptional.get();
        } catch (Exception e) {
            System.out.println("\n\n=========== Error finding user for balance check ===========\n\n");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error finding user"));
        }

        try {
            // Check if bank account is linked and PIN is set
            if (user.getBankAccountId() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "No bank account linked"));
            }
            if (user.getUpiPin() == null) {
                System.out.println("\n\n=========== User " + user.getId() + " trying balance check without setting PIN ===========\n\n");
                return ResponseEntity.badRequest().body(Map.of("message", "UPI PIN not set"));
            }


            // Verify UPI PIN
            try {
                if (!passwordEncoder.matches(upiPin, user.getUpiPin())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid UPI PIN"));
                }
            } catch (Exception e) {
                System.out.println("\n\n=========== Error matching UPI PIN during balance check ===========\n\n");
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Error verifying UPI PIN"));
            }


            // Call bank service for balance
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Token", INTERNAL_TOKEN);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            String accountUrl = "http://bank-service:8081/api/account/" + user.getBankAccountId();
            try {
                ResponseEntity<Map> accountResponse = restTemplate.exchange(
                        accountUrl,
                        HttpMethod.GET, entity, Map.class);

                if (accountResponse.getStatusCode() == HttpStatus.OK && accountResponse.getBody() != null && accountResponse.getBody().get("balance") != null) {
                    try {
                        Double balance = ((Number) accountResponse.getBody().get("balance")).doubleValue();
                        return ResponseEntity.ok(Map.of("balance", balance));
                    } catch (ClassCastException | NullPointerException e) {
                        System.out.println("\n\n=========== Error casting balance from bank service during balance check ===========\n\n");
                        System.out.println("User: " + user.getId() + ", Account: " + user.getBankAccountId());
                        System.out.println("Response Body: " + accountResponse.getBody());
                        e.printStackTrace();
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to process balance information from bank"));
                    }
                } else {
                    System.out.println("\n\n=========== Non-OK response or missing balance from bank service during balance check for user " + user.getId() + " ===========\n\n");
                    System.out.println("URL: " + accountUrl);
                    System.out.println("Status Code: " + accountResponse.getStatusCode());
                    System.out.println("Response Body: " + accountResponse.getBody());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Failed to retrieve balance from bank"));
                }

            } catch (RestClientException e) {
                System.out.println("\n\n=========== Error calling bank service for balance check ===========\n\n");
                System.out.println("URL: " + accountUrl);
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "Could not connect to bank service to check balance"));
            }

        } catch (Exception e) { // Catch any other unexpected errors
            System.out.println("\n\n=========== Unexpected error during checkBalance process ===========\n\n");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "An unexpected error occurred while checking balance"));
        }
    }

    // generateOtp remains the same, less likely to fail critically
    private String generateOtp() {
        // Simple OTP generation, less prone to exceptions unless Math.random behaves unexpectedly (highly unlikely)
        return String.valueOf((int) (Math.random() * 9000) + 1000);
    }
}