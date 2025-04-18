package com.xai.upi.client.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.xai.upi.client.model.TransactionRequest;
import com.xai.upi.client.security.CustomUserDetails;
import com.xai.upi.client.model.User;
import com.xai.upi.client.model.Notification;
import com.xai.upi.client.service.UserService;
import com.xai.upi.client.service.UPIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.xai.upi.client.service.NotificationService;
import com.xai.upi.client.model.TransactionDTO;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/transaction")
public class TransactionController {

    @Autowired
    private UserService userService;

    @Autowired
    private UPIService upiService;

    @Autowired
    private NotificationService notificationService;


    @PostMapping("/request")
    public String requestMoney(
            @RequestParam String receiverPhone,
            @RequestParam double amount,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        String senderUpiId = userService.getupiId(userDetails.getEmail());
        User receiver = upiService.searchUser(receiverPhone);
        if (receiver == null) {
            model.addAttribute("error", "Receiver not found");
            return "dashboard";
        }
        notificationService.createNotification(receiver.getId(), senderUpiId, amount);
        model.addAttribute("message", "Money request sent successfully");
        return "redirect:/dashboard";
    }

    @GetMapping("/payRequest")
    public String payRequest(@RequestParam String id, Model model) {
        Notification notification = notificationService.getPendingNotifications(null)
                .stream()
                .filter(n -> n.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        TransactionRequest request = new TransactionRequest();
        request.setSenderUpiId(null); // Will be set in form submission
        request.setReceiverPhone(notification.getRequesterUpiId());
        request.setAmount(notification.getAmount());

        model.addAttribute("transactionRequest", request);
        return "transaction";
    }

    @GetMapping("/notifications")
    public String getNotifications(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        List<Notification> notifications = notificationService.getPendingNotifications(userDetails.getUserId());
        model.addAttribute("notifications", notifications);
        return "notifications"; // Assuming notifications.html exists
    }


    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String userId = userDetails.getUserId();
        try {
            Map<String, Object> userData = userService.getUserData(userId);
            model.addAttribute("user", userData);
            model.addAttribute("balance", userData.get("balance"));
            List<TransactionDTO> transactions = upiService.getTransactions((String) userData.get("upiId"));
            model.addAttribute("transactions", transactions);
            return "dashboard";
        } catch (Exception e) {
            model.addAttribute("error", "Error fetching dashboard: " + e.getMessage());
            return "result";
        }
    }
    @PostMapping("/bankTransfer")
    public String bankTransferSubmit(
            @RequestParam String accountNumber,
            @RequestParam String ifscCode,
            @RequestParam double amount,
            @RequestParam String upiPin,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        String email = userDetails.getEmail();
        String senderUpiId = userService.getupiId(email);

        // Fetch user data as a Map and extract bankName
        Map<String, Object> userData = userService.getUserData(userDetails.getUserId());
        String bankName = (String) userData.get("bankName"); // Adjust key based on your data structure

        // Validate IFSC matches bankName
        if (!ifscCode.equalsIgnoreCase(bankName)) {
            model.addAttribute("error", "IFSC code must match the bank name");
            return "bankTransfer";
        }

        TransactionRequest request = new TransactionRequest();
        request.setSenderUpiId(senderUpiId);
        request.setReceiverPhone(accountNumber);
        request.setAmount(amount);
        request.setUpiPin(upiPin);

        try {
            Map<String, String> response = upiService.performTransaction(request);
            if (response.get("message").contains("success")) {
                model.addAttribute("message", "Bank transfer completed successfully");
                return "result";
            } else {
                model.addAttribute("error", response.get("message"));
                return "bankTransfer";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Bank transfer failed: " + e.getMessage());
            return "bankTransfer";
        }
    }

@GetMapping("/form")
public String transactionForm(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(required = false) String receiver,
        Model model
) {
    String userId = userDetails.getUserId();
    try {
        Map<String, Object> userData = userService.getUserData(userId);
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setSenderUpiId((String) userData.get("upiId"));
        if (receiver != null) {
            transactionRequest.setReceiverPhone(receiver);
        }
        model.addAttribute("transactionRequest", transactionRequest);
        return "transaction";
    } catch (Exception e) {
        model.addAttribute("error", "Error loading transaction form: " + e.getMessage());
        return "result";
    }
}

    @PostMapping("/form")
    public String transactionSubmit(@AuthenticationPrincipal CustomUserDetails userDetails,@ModelAttribute TransactionRequest request, Model model) {
        if (request == null) {
            System.out.println("TransactionRequest is null");
            model.addAttribute("error", "Invalid transaction request");
            model.addAttribute("transactionRequest", new TransactionRequest());
            return "transaction";
        }

        if (upiService == null) {
            System.out.println("UPIService is null");
            model.addAttribute("error", "Service unavailable");
            model.addAttribute("transactionRequest", request);
            return "transaction";
        }
        if (request.getUpiPin() == null){
            System.out.println("UpiPin is null");
            model.addAttribute("transactionRequest",request);
            return "transaction";
        }
        String email = userDetails.getEmail();
        request.setSenderUpiId(userService.getupiId(email));

        try {
            System.out.println("Submitting transaction: " + request);
            Map<String, String> response = upiService.performTransaction(request);
            if (response == null || response.get("message") == null) {
                System.out.println("Transaction response is null or missing message");
                model.addAttribute("error", "Transaction failed");
                model.addAttribute("transactionRequest", request);
                return "transaction";
            }
            model.addAttribute("message", response.get("message"));
            return "result";
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error occurred";
            System.out.println("Transaction error: " + errorMsg + ", Exception: " + e.getClass().getName());
            e.printStackTrace(); // Log full stack trace for debugging
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
public void generateQr(@AuthenticationPrincipal CustomUserDetails userDetails, HttpServletResponse response) throws WriterException, IOException {
    String upiId = userDetails.getUpiId();
    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    BitMatrix bitMatrix = qrCodeWriter.encode(upiId, BarcodeFormat.QR_CODE, 200, 200);
    response.setContentType("image/png");
    MatrixToImageWriter.writeToStream(bitMatrix, "PNG", response.getOutputStream());
}

}