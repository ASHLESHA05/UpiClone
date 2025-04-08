package com.xai.upi.client.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.xai.upi.client.model.SetUpiPinRequest;
import com.xai.upi.client.model.SignUpRequest;
import com.xai.upi.client.model.TransactionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
public class ClientController {

    @Autowired
    private RestTemplate restTemplate;

    private static final String INTERNAL_TOKEN = "uyguyfgbsvbcug76t7632$%@^@t";

    @GetMapping("/")
    public String home() {
        return "index";
    }

    // Signup Form
    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("signupRequest", new SignUpRequest());
        return "signup";
    }

    @PostMapping("/signup")
    public String signupSubmit(@ModelAttribute SignUpRequest signupRequest, Model model) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        HttpEntity<SignUpRequest> entity = new HttpEntity<>(signupRequest, headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://npci-service:8082/api/ipc/signup",
                    HttpMethod.POST, entity, Map.class);
            Map<String, String> responseBody = response.getBody();
            model.addAttribute("message", responseBody.get("message"));
            return "login";
        } catch (HttpClientErrorException e) {
            model.addAttribute("error", e.getResponseBodyAsString());
            return "signup";
        }
    }

    // Login Form
    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam String email, @RequestParam String password, HttpSession session, Model model) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("email", email, "password", password), headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://npci-service:8082/api/ipc/login",
                    HttpMethod.POST, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (response.getStatusCode() == HttpStatus.OK) {
                String userId = (String) responseBody.get("userId");
                session.setAttribute("userId", userId);
                String bankAccountId = (String) responseBody.get("bankAccountId");
                if (bankAccountId == null || bankAccountId.isEmpty()) {
                    return "redirect:/linkBankAccount?userId=" + userId;
                }
                return "redirect:/dashboard";
            }
            model.addAttribute("error", responseBody.get("message"));
            return "login";
        } catch (HttpClientErrorException e) {
            model.addAttribute("error", "Login failed: " + e.getMessage());
            return "login";
        }
    }

    // Link Bank Account Form
    @GetMapping("/linkBankAccount")
    public String linkBankAccountForm(@RequestParam String userId, Model model) {
        model.addAttribute("userId", userId);
        List<String> banks = List.of("sbi", "hdfc", "icici", "axis", "pnb", "bob", "canara", "union", "kotak", "yes");
        model.addAttribute("banks", banks);
        return "linkBankAccount";
    }

    @PostMapping("/linkBankAccount")
    public String linkBankAccountSubmit(@RequestParam String userId, @RequestParam String bankName,
                                        @RequestParam String atmNumber, @RequestParam String cvv, Model model) {
        if (!atmNumber.matches("\\d{16}") || !cvv.matches("\\d{3}")) {
            model.addAttribute("error", "Invalid ATM number (16 digits) or CVV (3 digits)");
            return "linkBankAccount";
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        Map<String, String> requestBody = Map.of("userId", userId, "bankName", bankName, "atmNumber", atmNumber, "cvv", cvv);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://npci-service:8082/api/ipc/linkBankAccount",
                    HttpMethod.POST, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return "redirect:/setUpiPin?userId=" + userId;
            }
            model.addAttribute("error", response.getBody().get("message"));
            return "linkBankAccount";
        } catch (HttpClientErrorException e) {
            model.addAttribute("error", "Error linking bank account: " + e.getMessage());
            return "linkBankAccount";
        }
    }

    // Set UPI PIN Form
    @GetMapping("/setUpiPin")
    public String setUpiPinForm(@RequestParam String userId, Model model) {
        SetUpiPinRequest request = new SetUpiPinRequest();
        request.setUserId(userId);
        model.addAttribute("setUpiPinRequest", request);
        return "setUpiPin";
    }

    @PostMapping("/setUpiPin")
    public String setUpiPinSubmit(@ModelAttribute SetUpiPinRequest request, Model model) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        HttpEntity<SetUpiPinRequest> entity = new HttpEntity<>(request, headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://npci-service:8082/api/ipc/setUpiPin",
                    HttpMethod.POST, entity, Map.class);
            model.addAttribute("message", response.getBody().get("message"));
            return "redirect:/dashboard";
        } catch (HttpClientErrorException e) {
            model.addAttribute("error", "Error setting UPI PIN: " + e.getMessage());
            return "setUpiPin";
        }
    }

    // Dashboard
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> userResponse = restTemplate.exchange(
                    "http://npci-service:8082/api/ipc/user?userId=" + userId,
                    HttpMethod.GET, entity, Map.class);
            Map<String, Object> userData = userResponse.getBody();
            if (userData == null || userData.containsKey("message")) {
                return "redirect:/login";
            }
            model.addAttribute("user", userData);
            model.addAttribute("balance", userData.get("balance"));
            ResponseEntity<List> transactionsResponse = restTemplate.exchange(
                    "http://npci-service:8082/api/ipc/transactions?upiId=" + userData.get("upiId"),
                    HttpMethod.GET, entity, List.class);
            model.addAttribute("transactions", transactionsResponse.getBody());
            return "dashboard";
        } catch (HttpClientErrorException e) {
            model.addAttribute("error", "Error fetching dashboard: " + e.getMessage());
            return "result";
        }
    }

    // Transaction Form
    @GetMapping("/transaction")
    public String transactionForm(HttpSession session, Model model) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> userResponse = restTemplate.exchange(
                    "http://npci-service:8082/api/ipc/user?userId=" + userId,
                    HttpMethod.GET, entity, Map.class);
            Map<String, Object> userData = userResponse.getBody();
            if (userData == null || userData.containsKey("message")) {
                return "redirect:/login";
            }
            TransactionRequest transactionRequest = new TransactionRequest();
            transactionRequest.setSenderUpiId((String) userData.get("upiId"));
            model.addAttribute("transactionRequest", transactionRequest);
            return "transaction";
        } catch (HttpClientErrorException e) {
            model.addAttribute("error", "Error loading transaction form: " + e.getMessage());
            return "result";
        }
    }

    @PostMapping("/transaction")
    public String transactionSubmit(@ModelAttribute TransactionRequest request, HttpSession session, Model model) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        HttpEntity<TransactionRequest> entity = new HttpEntity<>(request, headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://npci-service:8082/api/ipc/transaction",
                    HttpMethod.POST, entity, Map.class);
            model.addAttribute("message", response.getBody().get("message"));
            return "result";
        } catch (HttpClientErrorException e) {
            String errorMsg = e.getResponseBodyAsString();
            if (errorMsg.contains("Receiver not found")) {
                model.addAttribute("error", "User not found");
            } else if (errorMsg.contains("Insufficient balance")) {
                model.addAttribute("error", "Insufficient balance");
            } else {
                model.addAttribute("error", errorMsg);
            }
            model.addAttribute("transactionRequest", request);
            return "transaction";
        }
    }

    // Check Balance Form
    @GetMapping("/checkBalance")
    public String checkBalanceForm(HttpSession session, Model model) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }
        model.addAttribute("userId", userId);
        return "checkBalance";
    }

    @PostMapping("/checkBalance")
    public String checkBalanceSubmit(@RequestParam String userId, @RequestParam String upiPin, Model model) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("userId", userId, "upiPin", upiPin), headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://npci-service:8082/api/ipc/checkBalance",
                    HttpMethod.POST, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                model.addAttribute("balance", response.getBody().get("balance"));
                return "balanceResult";
            }
            model.addAttribute("error", response.getBody().get("message"));
            return "checkBalance";
        } catch (HttpClientErrorException e) {
            model.addAttribute("error", "Error checking balance: " + e.getMessage());
            return "checkBalance";
        }
    }

    // QR Code Generation
    @GetMapping("/generateQr")
    public void generateQr(HttpSession session, HttpServletResponse response) throws WriterException, IOException {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            response.sendRedirect("/login");
            return;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> userResponse = restTemplate.exchange(
                "http://npci-service:8082/api/ipc/user?userId=" + userId,
                HttpMethod.GET, entity, Map.class);
        String upiId = (String) userResponse.getBody().get("upiId");
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(upiId, BarcodeFormat.QR_CODE, 200, 200);
        response.setContentType("image/png");
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", response.getOutputStream());
    }
}