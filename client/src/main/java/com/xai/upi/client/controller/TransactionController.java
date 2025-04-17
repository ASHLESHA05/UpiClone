package com.xai.upi.client.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.xai.upi.client.model.TransactionRequest;
import com.xai.upi.client.security.CustomUserDetails;
import com.xai.upi.client.service.UserService;
import com.xai.upi.client.service.UPIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/transaction")
public class TransactionController {

    @Autowired
    private UserService userService;
    private UPIService upiService;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String userId = userDetails.getUserId();
        try {
            Map<String, Object> userData = userService.getUserData(userId);
            model.addAttribute("user", userData);
            model.addAttribute("balance", userData.get("balance"));
            List<Map> transactions = upiService.getTransactions((String) userData.get("upiId"));
            model.addAttribute("transactions", transactions);
            return "dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Error fetching dashboard: " + e.getMessage());
            return "result";
        }
    }

    @GetMapping("/form")
    public String transactionForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String userId = userDetails.getUserId();
        try {
            Map<String, Object> userData = userService.getUserData(userId);
            TransactionRequest transactionRequest = new TransactionRequest();
            transactionRequest.setSenderUpiId((String) userData.get("upiId"));
            model.addAttribute("transactionRequest", transactionRequest);
            return "transaction";
        } catch (Exception e) {
            model.addAttribute("error", "Error loading transaction form: " + e.getMessage());
            return "result";
        }
    }

    @PostMapping("/form")
    public String transactionSubmit(@ModelAttribute TransactionRequest request, Model model) {
        try {
            Map<String, String> response = upiService.performTransaction(request);
            model.addAttribute("message", response.get("message"));
            return "result";
        } catch (Exception e) {
            String errorMsg = e.getMessage();
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

    @GetMapping("/checkBalance")
    public String checkBalanceForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String userId = userDetails.getUserId();
        model.addAttribute("userId", userId);
        return "checkBalance";
    }

    @PostMapping("/checkBalance")
    public String checkBalanceSubmit(@RequestParam String userId, @RequestParam String upiPin, Model model) {
        try {
            Map<String, Object> response = upiService.checkBalance(userId, upiPin);
            model.addAttribute("balance", response.get("balance"));
            return "balanceResult";
        } catch (Exception e) {
            model.addAttribute("error", "Error checking balance: " + e.getMessage());
            return "checkBalance";
        }
    }

    @GetMapping("/generateQr")
    public void generateQr(@AuthenticationPrincipal CustomUserDetails userDetails, HttpServletResponse response)
            throws WriterException, IOException {
        String upiId = userDetails.getUpiId();
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(upiId, BarcodeFormat.QR_CODE, 200, 200);
        response.setContentType("image/png");
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", response.getOutputStream());
    }
}