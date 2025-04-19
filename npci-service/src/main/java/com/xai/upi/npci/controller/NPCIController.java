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
import java.util.*;

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
        headers.set("fonlt", "present");
        return headers;
    }

    @PostMapping("/ipc/signup")
    public ResponseEntity<String> signUp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");
        String phone = request.get("phone");
        String name = request.get("name");
        String userId = request.get("userId");

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already exists");
        }

        User user = new User();
        user.setEmail(email);
        System.out.println("In NPCI server save userID"+userId);
        user.setUserId(userId);
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

        Optional<User> userOptional = userRepository.findByUserId(userId);
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

        Optional<User> userOptional = userRepository.findByUserId(userId);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User user = userOptional.get();
        user.setUpiPin(passwordEncoder.encode(upiPin));
        //Here use the email to get the account number and add it



        userRepository.save(user);

        return ResponseEntity.ok("UPI PIN set successfully");
    }

    @PostMapping(value = "/ipc/transaction", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> makeTransaction(@RequestBody Map<String, String> request) {
        String fromUserId = request.get("senderUpiId");
        String toUpiId = request.get("receiverPhone");
        String amountStr = request.get("amount");
        String upiPin = request.get("upiPin");
        System.out.println(fromUserId);
        System.out.println(toUpiId);
        System.out.println(request);

        Optional<User> fromUserOptional = userRepository.findByUpiIdOrPhoneOrBankAccountId(fromUserId, fromUserId,fromUserId);
        Optional<User> toUserOptional = userRepository.findByUpiIdOrPhoneOrBankAccountId(toUpiId, toUpiId,toUpiId);
        System.out.println(toUserOptional.isPresent());
        System.out.println(fromUserOptional.isPresent());

        if (!fromUserOptional.isPresent() || !toUserOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        }

        User fromUser = fromUserOptional.get();
        if (!passwordEncoder.matches(upiPin, fromUser.getUpiPin())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid UPI PIN"));
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid amount"));
        }

        User fuser = fromUserOptional.get();
        User tuser = toUserOptional.get();
        String bnkName = fuser.getBankName();
        String bankid = fuser.getBankAccountId();

        HttpEntity<Map<String, String>> debitEntity = new HttpEntity<>(Map.of("bankName", bnkName, "accountNumber", bankid, "amount", amountStr), getHeaders());
        ResponseEntity<String> debitResponse = restTemplate.exchange(BANK_BASE_URL + "/ipc/debit", HttpMethod.POST, debitEntity, String.class);

        if (debitResponse.getStatusCode() != HttpStatus.OK) {
            return ResponseEntity.status(debitResponse.getStatusCode()).body(Map.of("message", debitResponse.getBody()));
        }

        String tbnkName = tuser.getBankName();
        String tbankid = tuser.getBankAccountId();

        HttpEntity<Map<String, String>> creditEntity = new HttpEntity<>(Map.of("bankName", tbnkName, "accountNumber", tbankid, "amount", amountStr), getHeaders());
        ResponseEntity<String> creditResponse = restTemplate.exchange(BANK_BASE_URL + "/ipc/credit", HttpMethod.POST, creditEntity, String.class);

        if (creditResponse.getStatusCode() != HttpStatus.OK) {
            HttpEntity<Map<String, String>> rollbackEntity = new HttpEntity<>(Map.of("bankName", bnkName, "accountNumber", bankid, "amount", amountStr), getHeaders());
            restTemplate.exchange(BANK_BASE_URL + "/ipc/credit", HttpMethod.POST, rollbackEntity, String.class);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Transaction failed: Credit error"));
        }

        if (creditResponse.getStatusCode() == HttpStatus.OK) {
            LocalDateTime time = LocalDateTime.now();
            Date date = Date.from(time.atZone(java.time.ZoneId.systemDefault()).toInstant());

            Transaction transaction = new Transaction();
            transaction.setFromUserId(fromUserId);
            transaction.setToUpiId(tuser.getUpiId());
            transaction.setAmount(amount);
            transaction.setTimestamp(date);
            transaction.setStatus("SUCCESS");
            transactionRepository.save(transaction);
            return ResponseEntity.ok(Map.of("message", "Transaction successful"));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Transaction failed"));
    }

    @GetMapping("/ipc/transactions")
    public ResponseEntity<List<Transaction>> getTransactionsInternal(@RequestParam String upiId) {
        List<Transaction> transactions = transactionRepository.findByFromUserIdOrToUpiId(upiId, upiId);
        if (!transactions.isEmpty()) {
            Transaction t = transactions.get(0);
            System.out.println("ID: " + t.getId());
            System.out.println("From User ID: " + t.getFromUserId());
            System.out.println("To User ID: " + t.getToUserId());
            System.out.println("Sender UPI ID: " + t.getSenderUpiId());
            System.out.println("Receiver UPI ID: " + t.getReceiverUpiId());
            System.out.println("To UPI ID: " + t.getToUpiId());
            System.out.println("Amount: " + t.getAmount());
            System.out.println("Status: " + t.getStatus());
            System.out.println("Timestamp: " + t.getTimestamp());
        }
        else{
            System.out.println("No transactions found\n\n"+transactions+"\n\n");
        }
        return ResponseEntity.ok(transactions);
    }


    @PostMapping("/ipc/checkBalance")
    public ResponseEntity<Map<String, Object>> checkBalance(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String upiPin = request.get("upiPin");

        Optional<User> userOptional = userRepository.findByUserId(userId);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        User user = userOptional.get();
        if (!passwordEncoder.matches(upiPin, user.getUpiPin())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid UPI PIN"));
        }

        HttpEntity<String> entity = new HttpEntity<>(getHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(BANK_BASE_URL + "/account/" + userId, HttpMethod.GET, entity, Map.class);
        return ResponseEntity.ok(response.getBody());
    }

    @PostMapping("/getAcc-phn-name")
    public ResponseEntity<List<Map>> getAccdata(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String bankName = request.get("bankName");
        if (phone == null || bankName == null) {
            return ResponseEntity.badRequest().build();
        }
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("phone", phone, "bankName", bankName), getHeaders());
        ResponseEntity<List> response = restTemplate.exchange(BANK_BASE_URL + "/ipc/getAccdata", HttpMethod.POST, entity, List.class);
        return ResponseEntity.ok(response.getBody());
    }

    @PostMapping("/getOtp")
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
        System.out.println("\n\n==========OTP=========\n\n\t"+otp+"\n===================\n\n");
        user.setOtp(otp);
        user.setOtpGeneratedAt(LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok(Integer.parseInt(otp));
    }

    @PostMapping("/verifyOtp")
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

    @PostMapping("/getCardData")
    public ResponseEntity<List<Map>> getCardData(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String bankName = request.get("bankName");
        if (email == null || bankName == null) {
            return ResponseEntity.badRequest().build();
        }
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("email", email, "bankName", bankName), getHeaders());
        ResponseEntity<List> response = restTemplate.exchange(BANK_BASE_URL + "/ipc/getCardData", HttpMethod.POST, entity, List.class);
        return ResponseEntity.ok(response.getBody());
    }

    @PostMapping("/verifyCard")
    public ResponseEntity<Boolean> verifyCardData(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String bankName = request.get("bankName");
        String cvv = request.get("cvv");
        String cardNumber = request.get("cardNumber");
        String atmPin = request.get("atmPin");
        if (email == null || bankName == null || cvv == null || cardNumber == null) {
            return ResponseEntity.badRequest().build();
        }
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("email", email, "bankName", bankName, "cvv", cvv, "cardNumber", cardNumber), getHeaders());
        ResponseEntity<List> cardResponse = restTemplate.exchange(BANK_BASE_URL + "/ipc/getCardData", HttpMethod.POST, new HttpEntity<>(Map.of("email", email, "bankName", bankName), getHeaders()), List.class);
        List<Map> cards = cardResponse.getBody();
        if (cards != null) {
            for (Map card : cards) {
                if (card.get("atmCardNumber").equals(cardNumber) && card.get("cvv").equals(cvv) && card.get("pin").equals(atmPin)) {
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
    @PostMapping("/saveAccoutInfo")
    public ResponseEntity<Boolean> saveAccountInfo(@RequestBody Map<String, String> request){
        String email = request.get("email");
        String accountNumber = request.get("accountNumber");
        String bankName = request.get("bankName");

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        User user = userOptional.get();
        user.setBankName(bankName);
        user.setBankAccountId(accountNumber);
        userRepository.save(user);

        return ResponseEntity.ok(true);
    }


    @PostMapping("/generateUpiId")
    public ResponseEntity<String> generateAndSaveUpiId(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String bankName = request.get("bankName");
        String phone = request.get("phone");

        if (email == null || bankName == null || phone == null) {
            return ResponseEntity.badRequest().body("Email, bankName, and phone are required");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        String lastFourDigits = phone.length() > 4 ? phone.substring(phone.length() - 4) : phone;
        String upiId = email.split("@")[0] + lastFourDigits + "@" + bankName.toLowerCase() + ".MYpay";

        User user = userOptional.get();
        user.setUpiId(upiId);
        userRepository.save(user);

        return ResponseEntity.ok(upiId);
    }

    @PostMapping("/getUpiId")
    public ResponseEntity<String> getUpiId(@RequestBody Map<String, String> request) {
        String bankName = request.get("bankName");
        String email = request.get("email");
        String phone = request.get("phone");

        if (bankName == null || email == null || phone == null) {
            return ResponseEntity.badRequest().body("bankName, email, and phone are required");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User user = userOptional.get();
        if (user.getUpiId() != null && !user.getUpiId().isEmpty()) {
            return ResponseEntity.ok(user.getUpiId());
        }

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("email", email, "phone", phone, "bankName", bankName), getHeaders());
        ResponseEntity<String> response = restTemplate.exchange(BANK_BASE_URL + "/getUpiID", HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            user.setUpiId(response.getBody());
            userRepository.save(user);
            return ResponseEntity.ok(response.getBody());
        }

        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    @PostMapping("/searchUser")
    public ResponseEntity<User> searchUser(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        Optional<User> userByPhone = userRepository.findByPhone(query);
        if (userByPhone.isPresent()) {
            return ResponseEntity.ok(userByPhone.get());
        }
        Optional<User> userByUpiId = userRepository.findByUpiId(query);
        return userByUpiId.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.ok(null));
    }

    @PostMapping("/addFriend")
    public ResponseEntity<String> addFriend(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String identifier = request.get("identifier");

        Optional<User> userOptional = userRepository.findByUserId(userId);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User user = userOptional.get();
        Optional<User> friendOptional = userRepository.findByPhone(identifier).isPresent() ? userRepository.findByPhone(identifier) : userRepository.findByUpiId(identifier);
        if (!friendOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Friend not found");
        }

        User friend = friendOptional.get();
        List<String> friends = user.getFriends() != null ? user.getFriends() : new ArrayList<>();
        if (!friends.contains(friend.getId())) {
            friends.add(friend.getId());
            user.setFriends(friends);
            userRepository.save(user);
        }
        return ResponseEntity.ok("Friend added successfully");
    }

    @PostMapping("/getFriends")
    public ResponseEntity<List<User>> getFriends(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        Optional<User> userOptional = userRepository.findByUserId(userId);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        User user = userOptional.get();
        List<String> friendIds = user.getFriends() != null ? user.getFriends() : new ArrayList<>();
        List<User> friends = new ArrayList<>();
        for (String friendId : friendIds) {
            Optional<User> user2 = userRepository.findById(friendId);
            if(user2.isPresent()) {
                friends.add(user2.get());
            }
        }
        return ResponseEntity.ok(friends);
    }

    @PostMapping("/addFamilyMember")
    public ResponseEntity<String> addFamilyMember(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        String identifier = request.get("identifier");

        Optional<User> userOptional = userRepository.findByUserId(userId);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User user = userOptional.get();
        Optional<User> memberOptional = userRepository.findByPhone(identifier).isPresent() ? userRepository.findByPhone(identifier) : userRepository.findByUpiId(identifier);
        if (!memberOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Family member not found");
        }

        User member = memberOptional.get();
        List<String> familyMembers = user.getFamilyMembers() != null ? user.getFamilyMembers() : new ArrayList<>();
        if (!familyMembers.contains(member.getId())) {
            familyMembers.add(member.getId());
            user.setFamilyMembers(familyMembers);
            userRepository.save(user);
        }
        return ResponseEntity.ok("Family member added successfully");
    }

    @PostMapping("/getFamilyMembers")
    public ResponseEntity<List<User>> getFamilyMembers(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        System.out.println("Gen Family members invoked");
        System.out.println("userId: " + userId);

        Optional<User> userOptional = userRepository.findByUserId(userId);


        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        User user = userOptional.get();
        System.out.println("Found family members Nigga ,"+user);
        List<String> memberIds = user.getFamilyMembers() != null ? user.getFamilyMembers() : new ArrayList<>();
        List<User> members = new ArrayList<>();
        System.out.println("memberIds "+memberIds);
        for (String memberId : memberIds) {
            Optional<User> user1 = userRepository.findById(memberId);
            if(user1.isPresent()){
                members.add(user1.get());
            }
        }
        System.out.println("members "+members);
        return ResponseEntity.ok(members);
    }

    @PostMapping("/verifyPin")
    public ResponseEntity<Boolean> verifyPin(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String upiPin = request.get("upiPin");
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (!userOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        User user = userOptional.get();
        return ResponseEntity.ok(passwordEncoder.matches(upiPin, user.getUpiPin()));
    }

    private String generateUpiId(String email) {
        return email.split("@")[0] + "@mypay";
    }

    private String generateOtp() {
        return String.format("%04d", (int) (Math.random() * 9000) + 1000);
    }
}