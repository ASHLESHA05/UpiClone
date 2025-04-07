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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
public class ClientController {

    @Autowired
    private RestTemplate restTemplate;

    private static final String INTERNAL_TOKEN = "uyguyfgbsvbcug76t7632$%@^@t"; // Match NPCI and Bank

    @GetMapping("/")
    public String home() {
        return "index";
    }

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

        ResponseEntity<Map> response = restTemplate.exchange(
                "http://npci-service:8082/api/ipc/signup",
                HttpMethod.POST, entity, Map.class);
        Map<String, Object> responseBody = response.getBody();



        model.addAttribute("message", responseBody.get("message"));
        model.addAttribute("userId", responseBody.get("userId"));

        SetUpiPinRequest setUpiPinRequest = new SetUpiPinRequest();
        setUpiPinRequest.setUserId((String) responseBody.get("userId"));
        model.addAttribute("setUpiPinRequest", setUpiPinRequest);

        return "setUpiPin";
    }


    @PostMapping("/setUpiPin")
    public String setUpiPinSubmit(@ModelAttribute SetUpiPinRequest request, Model model) {
        System.out.println("\n\n\n\n\n===================In post mapping setupipin====================\n\n\n\n");
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        HttpEntity<SetUpiPinRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "http://npci-service:8082/api/ipc/setUpiPin",
                HttpMethod.POST, entity, Map.class);
        model.addAttribute("message", response.getBody().get("message"));
        return "result";
    }

    @GetMapping("/setUpiPin")
    public String setUpiPinForm(Model model, @RequestParam(required = false) String userId) {
        SetUpiPinRequest setUpiPinRequest = new SetUpiPinRequest();
        if (userId != null) {
            setUpiPinRequest.setUserId(userId); // Pre-fill userId from signup
        }
        model.addAttribute("setUpiPinRequest", setUpiPinRequest);
        return "setUpiPin";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, @RequestParam String upiId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Fetch user data (including balance) via NPCI IPC
        ResponseEntity<Map> userResponse = restTemplate.exchange(
                "http://npci-service:8082/api/ipc/user?upiId=" + upiId,
                HttpMethod.GET, entity, Map.class);
        Map<String, Object> userData = userResponse.getBody();

        // Fetch transactions via NPCI IPC
        ResponseEntity<List> transactionsResponse = restTemplate.exchange(
                "http://npci-service:8082/api/ipc/transactions?upiId=" + upiId,
                HttpMethod.GET, entity, List.class);
        List<Map<String, Object>> transactions = transactionsResponse.getBody();

        model.addAttribute("user", userData);
        model.addAttribute("balance", userData.get("balance"));
        model.addAttribute("transactions", transactions);
        return "dashboard";
    }

    @GetMapping("/transaction")
    public String transactionForm(Model model, @RequestParam String upiId) {
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setSenderUpiId(upiId); // Pre-fill sender UPI ID
        model.addAttribute("transactionRequest", transactionRequest);
        model.addAttribute("senderUpiId", upiId);
        return "transaction";
    }

    @PostMapping("/transaction")
    public String transactionSubmit(@ModelAttribute TransactionRequest request, Model model) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        HttpEntity<TransactionRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                "http://npci-service:8082/api/ipc/transaction",
                HttpMethod.POST, entity, Map.class);
        model.addAttribute("message", response.getBody().get("message"));
        return "result";
    }

    @GetMapping("/generateQr")
    public void generateQr(@RequestParam String upiId, HttpServletResponse response) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(upiId, BarcodeFormat.QR_CODE, 200, 200);
        response.setContentType("image/png");
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", response.getOutputStream());
    }
}