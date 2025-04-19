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
import com.xai.upi.client.model.NotificationDTO;
import com.xai.upi.client.service.UserService;
import com.xai.upi.client.service.UPIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.xai.upi.client.service.NotificationService;
import com.xai.upi.client.model.TransactionDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


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



    @GetMapping("/pendingRequests")
    public String getPendingRequests(Model model) {
        // Get current user's email from Spring Security
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName(); // Retrieves the email (or username) of the logged-in user

        // Fetch pending requests for the current user
        User user = userService.findUserByEmail(userEmail);
        List<NotificationDTO> pendingRequests = notificationService.getPendingRequestsByUser(user.getId());

        // Add attributes to the model
        model.addAttribute("userEmail", userEmail);
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("familyMode", false); // Assuming a method to check family mode

        return "pendingRequests"; // Maps to pendingRequests.html
    }
    @PostMapping("/approve/{id}")
    public String approveRequest(@PathVariable String id, Model model) {
        System.out.println("Approving request with ID: " + id);
        try {
            notificationService.approveRequest(id);
            model.addAttribute("message", "Request approved successfully");
            return "redirect:/transaction/pendingRequests";
        } catch (Exception e) {
            System.out.println("Error approving request: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Failed to approve request: " + e.getMessage());
            List<NotificationDTO> pendingRequests = notificationService.getPendingRequestsByUser(
                    userService.findUserByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).getId()
            );
            model.addAttribute("pendingRequests", pendingRequests);
            return "pendingRequests";
        }
    }

    @PostMapping("/reject/{id}")
    public String cancelRequest(@PathVariable String id, Model model) {
        System.out.println("Rejecting request with ID: " + id);
        try {
            notificationService.cancelRequest(id);
            model.addAttribute("message", "Request rejected successfully");
            return "redirect:/transaction/pendingRequests";
        } catch (Exception e) {
            System.out.println("Error rejecting request: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Failed to reject request: " + e.getMessage());
            List<NotificationDTO> pendingRequests = notificationService.getPendingRequestsByUser(
                    userService.findUserByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).getId()
            );
            model.addAttribute("pendingRequests", pendingRequests);
            return "pendingRequests";
        }
    }


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
        String email = receiver.getEmail();
        System.out.println("RecieverEmail= "+email);
        User user = userService.findUserByEmail(email);
        if (user == null) {
            System.out.println("user not found");
        }




        notificationService.createNotification(
                user.getId(),
                userDetails.getUserId(),
                senderUpiId,
                userDetails.getUpiId(),
                amount
        );
        model.addAttribute("message", "Money request sent successfully");
        return "redirect:/dashboard";
    }


//    @PostMapping("/requestSplit")
//    public String requestMoney(
//            @RequestBody Map<String, Object> payload,
//            @AuthenticationPrincipal CustomUserDetails userDetails,
//            Model model
//    ) {
//        System.out.println("requestMoney payload= ");
//        String receiverPhone = (String) payload.get("receiverPhone");
//        double amount = Double.parseDouble(payload.get("amount").toString());
//
//        String senderUpiId = userService.getupiId(userDetails.getEmail());
//        User receiver = upiService.searchUser(receiverPhone);
//
////        if (receiver == null) {
////            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Receiver not found");
////        }
//        String email = receiver.getEmail();
//        System.out.println("RecieverEmail= "+email);
//        User user = userService.findUserByEmail(email);
//        if (user == null) {
//            System.out.println("user not found");
//        }
//
//
//
//
//        notificationService.createNotification(
//                user.getId(),
//                userDetails.getUserId(),
//                senderUpiId,
//                userDetails.getUpiId(),
//                amount
//        );
//        model.addAttribute("message", "Money request sent successfully");
//        return "redirect:/dashboard";
//    }








    @GetMapping("/payRequest")
    public String payRequest(@RequestParam String id, Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
        Notification notification = notificationService.getPendingNotifications(userDetails.getUserId())
                .stream()
                .filter(n -> n.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        System.out.println("Inside payRequest");
        TransactionRequest request = new TransactionRequest();
        request.setReceiverPhone(notification.getRequesterUpiId());
        request.setAmount(notification.getAmount());
        request.setTransactionId(id);

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
            System.out.println("Inside dashboard Transactions"+transactions);
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
        System.out.println("Inside bankTransferSubmit:  "+userData);
        String bankName = (String) userData.get("bankName"); // Adjust key based on your data structure

        // Validate IFSC matches bankName
        if (!ifscCode.equalsIgnoreCase(bankName)) {
            model.addAttribute("error", "IFSC code must match the bank name");
            return "bankTransfer";
        }

//        User RevcUser = userService.getUserByBankandACN(bankName,accountNumber);
//        if (RevcUser == null) {
//            model.addAttribite("error","User Not found on that accoutNumber")
//            return "redirect:/dashboard";
//        }
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

            //Here Make The status as completed
            if (request.getTransactionId() != null) {
                notificationService.completeNotification(request.getTransactionId());
            }


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
        String upiUrl = "upi://pay?pa=" + upiId; // Construct UPI payment URL
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(upiUrl, BarcodeFormat.QR_CODE, 200, 200); // Encode UPI URL
        response.setContentType("image/png");
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", response.getOutputStream());
    }

}