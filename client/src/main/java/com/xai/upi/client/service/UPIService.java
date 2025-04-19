package com.xai.upi.client.service;

import com.xai.upi.client.model.Account;
import com.xai.upi.client.model.SetUpiPinRequest;
import com.xai.upi.client.model.TransactionRequest;
import com.xai.upi.client.model.User;
import com.xai.upi.client.model.temSave;
import com.xai.upi.client.model.TransactionDTO;
import com.xai.upi.client.repository.*;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import java.util.Collections;



import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class UPIService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private final UserStatusService userStatusService;

    @Value("${twilio.accountSid}")
    private String twilioAccountSid;

    @Value("${twilio.authToken}")
    private String twilioAuthToken;

    @Value("${twilio.phoneNumber}")
    private String twilioPhoneNumber;

    @Autowired

    public UPIService(RestTemplate restTemplate, UserStatusService userStatusService) {
        this.restTemplate = restTemplate;
        this.userStatusService = userStatusService;
    }

    private temSave tempSave;

    private static final String INTERNAL_TOKEN = "uyguyfgbsvbcug76t7632$%@^@t";
    private static final String BASE_URL = "http://localhost:8082/api";

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    public void linkBankAccount(String userId, String bankName, String atmNumber, String cvv) {
        Map<String, String> requestBody = Map.of(
                "userId", userId,
                "bankName", bankName,
                "atmNumber", atmNumber,
                "cvv", cvv
        );
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, getHeaders());
        restTemplate.exchange(BASE_URL + "/ipc/linkBankAccount", HttpMethod.POST, entity, Void.class);
    }

    public Map<String, String> setUpiPin(SetUpiPinRequest request) {
        HttpEntity<SetUpiPinRequest> entity = new HttpEntity<>(request, getHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(BASE_URL + "/ipc/setUpiPin", HttpMethod.POST, entity, Map.class);
        return response.getBody();
    }

    public List<TransactionDTO> getTransactions(String upiId) {
        HttpEntity<String> entity = new HttpEntity<>(getHeaders());
        System.out.println("Inside getTransactions"+upiId);
        ResponseEntity<List<com.xai.upi.client.model.Transaction>> response = restTemplate.exchange(
                BASE_URL + "/ipc/transactions?upiId=" + upiId,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<List<com.xai.upi.client.model.Transaction>>() {}
        );
//        System.out.println("Transaction: RES: = "+response.getBody());
        return response.getBody().stream().map(tx -> {
//            System.out.println("Transaction: RES ||tx = "+tx.getFromUserId()+" || "+tx.getToUpiId());

            User sender = userRepository.findByUpiId(tx.getFromUserId());
            User receiver = userRepository.findByUpiId(tx.getToUpiId());

            String senderName = null;
            String receiverName = null;



            if (sender != null) senderName = sender.getUsername();
            if (receiver != null) {
//                System.out.println("Reciever Name : " + receiver.getName());

                receiverName = receiver.getUsername();
            }

//            System.out.println("Sender: " + senderName);
//            System.out.println("Receiver: " + receiverName);

            return new TransactionDTO(tx, upiId, senderName, receiverName);
        }).collect(Collectors.toList());
    }



    public Map<String, String> performTransaction(TransactionRequest request) {
        HttpEntity<TransactionRequest> entity = new HttpEntity<>(request, getHeaders());
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    BASE_URL + "/ipc/transaction",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                return Map.of("message", "Transaction failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            if (errorMsg.contains("Insufficient balance")) {
                return Map.of("message", "Insufficient balance");
            } else if (errorMsg.contains("User not found")) {
                return Map.of("message", "Receiver not found");
            }
            return Map.of("message", "Transaction error: " + errorMsg);
        }
    }





    public Map<String, Object> checkBalance(String userId, String upiPin) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("userId", userId, "upiPin", upiPin), getHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(BASE_URL + "/ipc/checkBalance", HttpMethod.POST, entity, Map.class);
        return response.getBody();
    }

    public List<Account> getAccountdetails(String phone, String bankName) {
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("phone", phone, "bankName", bankName), getHeaders());

            ResponseEntity<List<Account>> response = restTemplate.exchange(
                    BASE_URL + "/getAcc-phn-name",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<List<Account>>() {}
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            }
            return null;
        }

    public void tempSaveaccountData(String email, String bankName, String accountNumber) {
        try {
            tempSave = new temSave(email, bankName, accountNumber);
            //Save it in user databse of npci
            //Making an Http request
            HttpEntity<Map<String,String>> entity = new HttpEntity<>(Map.of("email", email, "bankName", bankName, "accountNumber",accountNumber), getHeaders());
            ResponseEntity<Boolean> response = restTemplate.exchange(BASE_URL + "/saveAccoutInfo", HttpMethod.POST, entity, Boolean.class);
//            return response.getBody();


        } catch (Exception e) {
            System.out.println("\n\n========ERROR==\n" + e + "\n==========\n\n");
        }
    }

    public Integer getotp(String email) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("email", email), getHeaders());
        ResponseEntity<Integer> response = restTemplate.exchange(BASE_URL + "/getOtp", HttpMethod.POST, entity, Integer.class);
        return response.getBody();
    }

    public boolean verifyOTP(String email, String otp) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("email", email, "otp", otp), getHeaders());
        ResponseEntity<Boolean> response = restTemplate.exchange(BASE_URL + "/verifyOtp", HttpMethod.POST, entity, Boolean.class);
        return response.getBody();
    }

    public List<Map<String, Object>> getcardData(String email, String bankName) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("email", email, "bankName", bankName), getHeaders());
        ResponseEntity<List> response = restTemplate.exchange(BASE_URL + "/getCardData", HttpMethod.POST, entity, List.class);
        return response.getBody();
    }

    public boolean verifyCard(String email, String bankName, String cvv, String cardNumber, String atmPin) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("email", email, "bankName", bankName, "cvv", cvv, "cardNumber", cardNumber, "atmPin", atmPin), getHeaders());
        ResponseEntity<Boolean> response = restTemplate.exchange(BASE_URL + "/verifyCard", HttpMethod.POST, entity, Boolean.class);
        return response.getBody();
    }

    public String generateUpiId(String bankName, String email, String phone) {
        Map<String, String> requestBody = Map.of("email", email, "phone", phone, "bankName", bankName);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, getHeaders());
        ResponseEntity<String> response = restTemplate.exchange(BASE_URL + "/generateUpiId", HttpMethod.POST, entity, String.class);
        return response.getBody();
    }

    public String getUpiId(String bankName, String email, String phone) {
        Map<String, String> requestBody = Map.of("email", email, "phone", phone, "bankName", bankName);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, getHeaders());
        ResponseEntity<String> response = restTemplate.exchange(BASE_URL + "/getUpiId", HttpMethod.POST, entity, String.class);

        //Get the uer and update UPI id field
        String upiId = response.getBody();
        User user = userRepository.findByEmail(email);
        if (user != null) {
            user.setUpiId(upiId);
            userRepository.save(user); // save updated user
        }

        return response.getBody();
    }

    public boolean saveUPIdata(String email, String upiPin, String bankName) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("email", email, "upiPin", upiPin), getHeaders());
        ResponseEntity<Boolean> response = restTemplate.exchange(BASE_URL + "/saveUpiPin", HttpMethod.POST, entity, Boolean.class);
        if (response.getBody() != null && response.getBody()) {
            userStatusService.markUpiPinAsSet(email,bankName);
            return true;
        }
        return false;
    }

    public String getMaskedCardNumber(String email, String bankName) {
        List<Map<String, Object>> cardList = getcardData(email, bankName);
        if (cardList == null || cardList.isEmpty()) {
            return "No card available";
        }
        String cardNumber = (String) cardList.get(0).get("atmCardNumber");
        if (cardNumber != null && cardNumber.length() >= 8) {
            return cardNumber.substring(0, 4) + "XXXXXXXX" + cardNumber.substring(cardNumber.length() - 4);
        }
        return "Invalid card number";
    }

    public User searchUser(String query) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("query", query), getHeaders());
        ResponseEntity<User> response = restTemplate.exchange(BASE_URL + "/searchUser", HttpMethod.POST, entity, User.class);
        return response.getBody();
    }

    public void addFriend(String userId, String identifier) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("userId", userId, "identifier", identifier), getHeaders());
        restTemplate.exchange(BASE_URL + "/addFriend", HttpMethod.POST, entity, Void.class);
    }

    public List<User> getFriends(String userId) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("userId", userId), getHeaders());
        ResponseEntity<List<User>> response = restTemplate.exchange(
                BASE_URL + "/getFriends",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<List<User>>() {}
        );
        return response.getBody();
    }

    public void addFamilyMember(String userId, String identifier) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("userId", userId, "identifier", identifier), getHeaders());
        restTemplate.exchange(BASE_URL + "/addFamilyMember", HttpMethod.POST, entity, Void.class);
    }

    public List<User> getFamilyMembers(String userId) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("userId", userId), getHeaders());
        ResponseEntity<List<User>> response = restTemplate.exchange(
                BASE_URL + "/getFamilyMembers",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<List<User>>() {}
        );
        return response.getBody();
    }


    public void sendInvitation(String phone) {
        Twilio.init(twilioAccountSid, twilioAuthToken);
        Message message = Message.creator(
                new PhoneNumber(phone),
                new PhoneNumber(twilioPhoneNumber),
                "Join MyPay UPI! Sign up with your phone number to start using our services."
        ).create();
    }

    public boolean verifyPin(String email, String pin) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("email", email, "upiPin", pin), getHeaders());
        ResponseEntity<Boolean> response = restTemplate.exchange(BASE_URL + "/verifyPin", HttpMethod.POST, entity, Boolean.class);
        return response.getBody();
    }

    public String generateQrCode(String upiId) {
        try {
            String upiUrl = "upi://pay?pa=" + upiId + "&pn=Recipient&am=0.00&cu=INR";
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(upiUrl, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return Base64.getEncoder().encodeToString(pngOutputStream.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }
}