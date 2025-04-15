package com.xai.upi.client.service;

import com.xai.upi.client.model.Account;
import com.xai.upi.client.model.SetUpiPinRequest;
import com.xai.upi.client.model.TransactionRequest;
import com.xai.upi.client.model.temSave;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class UPIService {

    @Autowired
    private RestTemplate restTemplate;

    private temSave tempSave;

    private static final String INTERNAL_TOKEN = "uyguyfgbsvbcug76t7632$%@^@t";
    private static final String BASE_URL = "http://localhost:8082/api";

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        return headers;
    }

    public Map<String, String> setUpiPin(SetUpiPinRequest request) {
        HttpEntity<SetUpiPinRequest> entity = new HttpEntity<>(request, getHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(BASE_URL + "/ipc/setUpiPin", HttpMethod.POST, entity, Map.class);
        return response.getBody();
    }

    public List<Map> getTransactions(String upiId) {
        HttpEntity<String> entity = new HttpEntity<>(getHeaders());
        ResponseEntity<List> response = restTemplate.exchange(BASE_URL + "/ipc/transactions?upiId=" + upiId, HttpMethod.GET, entity, List.class);
        return response.getBody();
    }

    public Map<String, String> performTransaction(TransactionRequest request) {
        HttpEntity<TransactionRequest> entity = new HttpEntity<>(request, getHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(BASE_URL + "/ipc/transaction", HttpMethod.POST, entity, Map.class);
        return response.getBody();
    }

    public Map<String, Object> checkBalance(String userId, String upiPin) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("userId", userId, "upiPin", upiPin), getHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(BASE_URL + "/ipc/checkBalance", HttpMethod.POST, entity, Map.class);
        return response.getBody();
    }

    public List<Account> getAccountdetails(String phone, String bankName) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("phone", phone, "bankName", bankName), getHeaders());
        ResponseEntity<List> response = restTemplate.exchange(BASE_URL + "/getAcc-phn-name", HttpMethod.GET, entity, List.class);
        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        }
        return null;
    }

    public void tempSaveaccountData(String email, String bankName, String accountNumber) {
        try {
            tempSave = new temSave(email, bankName, accountNumber);
            System.out.println("tempSaveaccountData DONE");
        } catch (Exception e) {
            System.out.println("\n\n========ERROR==\n" + e + "\n==========\n\n");
        }
    }

    public Integer getotp(String email) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("email", email), getHeaders());
        ResponseEntity<Integer> response = restTemplate.exchange(BASE_URL + "/getOtp", HttpMethod.GET, entity, Integer.class);
        return response.getBody();
    }

    public boolean verifyOTP(String email, String otp) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("email", email, "otp", otp), getHeaders());
        ResponseEntity<Boolean> response = restTemplate.exchange(BASE_URL + "/verifyOtp", HttpMethod.GET, entity, Boolean.class);
        return response.getBody();
    }

    public List<String> getcardData(String email, String bankName) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("email", email, "bankName", bankName), getHeaders());
        ResponseEntity<List> response = restTemplate.exchange(BASE_URL + "/getCardData", HttpMethod.GET, entity, List.class);
        return response.getBody();
    }

    public boolean verifyCard(String email, String bankName, String cvv, String cardNumber) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("email", email, "bankName", bankName, "cvv", cvv, "cardNumber", cardNumber), getHeaders());
        ResponseEntity<Boolean> response = restTemplate.exchange(BASE_URL + "/verifyCard", HttpMethod.GET, entity, Boolean.class);
        return response.getBody();
    }

    public boolean saveUPIdata(String email, String upiPin) {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("email", email, "upiPin", upiPin), getHeaders());
        ResponseEntity<Boolean> response = restTemplate.exchange(BASE_URL + "/saveUpiPin", HttpMethod.POST, entity, Boolean.class);
        return response.getBody();
    }
}