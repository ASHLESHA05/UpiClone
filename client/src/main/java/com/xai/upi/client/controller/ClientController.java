package com.xai.upi.client.controller;

import com.xai.upi.client.model.Account;
import com.xai.upi.client.model.User;
import com.xai.upi.client.model.Notification;
import com.xai.upi.client.repository.UserRepository;
import com.xai.upi.client.security.CustomUserDetails;
import com.xai.upi.client.service.UPIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.xai.upi.client.service.NotificationService;
import com.xai.upi.client.model.TransactionDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity; // For ResponseEntity class
import org.springframework.http.HttpStatus; // For HttpStatus constants (e.g., HttpStatus.OK)
import org.springframework.web.bind.annotation.*; // For @RestController, @RequestMapping, @GetMapping, @PostMapping, etc.
import org.springframework.beans.factory.annotation.Autowired; // For @Autowired annotation
import java.util.List; // For List collections
import java.util.Map; // For Map collections

import java.util.List;
import java.util.Map;

@Controller
public class ClientController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UPIService upiService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @PostMapping("/settings/unlink")
    public String unlinkAccount(
            @RequestParam String upiPin,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model
    ) {
        String userId = userDetails.getUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Validate UPI PIN
//        if (!passwordEncoder.matches(upiPin, user.getUpiPin())) {
//            model.addAttribute("error", "Invalid UPI PIN");
//            return "settings";
//        }

        // Unlink bank account
        user.setBankName(null);
//        user.setBankAccountId(null);
        user.setUpiId(null);
        userRepository.save(user);

        model.addAttribute("message", "Bank account unlinked successfully");
        return "settings";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String userId = userDetails.getUserId();
        System.out.println("UserId = " + userId);
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        String accountNumber = "N/A";
        Double balance = 0.0;

        if (user.getPhone() != null && user.getBankName() != null) {
            List<Account> accounts = upiService.getAccountdetails(user.getPhone(), user.getBankName());
            if (accounts != null && !accounts.isEmpty()) {
                accountNumber = accounts.get(0).getAccountNumber();
                balance = accounts.get(0).getBalance();
            }
        }

        String upiId = upiService.getUpiId(user.getBankName(), user.getEmail(), user.getPhone());
        List<User> familyMembers = upiService.getFamilyMembers(userId);
        List<User> friends = upiService.getFriends(userId);
        List<Notification> notifications = notificationService.getPendingNotifications(userId);
        List<TransactionDTO> transactions = upiService.getTransactions(upiId);

        model.addAttribute("user", user);
        model.addAttribute("balance", balance);
        model.addAttribute("upiId", upiId);
        model.addAttribute("bank", user.getBankName());
        model.addAttribute("accountNumber", accountNumber);
        model.addAttribute("transactions", transactions);
        model.addAttribute("familyMembers", familyMembers);
        model.addAttribute("friends", friends);
        model.addAttribute("qrCode", upiService.generateQrCode(upiId));
        model.addAttribute("notifications", notifications);

        model.addAttribute("pendingRequests", notificationService.getPendingRequestsByUser(user.getId()));

        return "dashboard";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        User user = userRepository.findById(userDetails.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
        model.addAttribute("user", user);
        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam String name, @RequestParam String phone) {
        User user = userRepository.findById(userDetails.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
        user.setName(name);
        user.setPhone(phone);
        userRepository.save(user);
        return "redirect:/profile";
    }

    @GetMapping("/transactions")
    public String transactions(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String userId = userDetails.getUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        String upiId = upiService.getUpiId(user.getBankName(), user.getEmail(), user.getPhone());
        List<TransactionDTO> transactions = upiService.getTransactions(upiId);
//        System.out.println("transactions = " + transactions);

        model.addAttribute("transactions", transactions);
        return "transactions";
    }

    @GetMapping("/settings")
    public String settings(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String userId = userDetails.getUserId();
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        String accountNumber = "N/A";
        String upiId = upiService.getUpiId(user.getBankName(), user.getEmail(), user.getPhone());

        if (user.getPhone() != null && user.getBankName() != null) {
            List<Account> accounts = upiService.getAccountdetails(user.getPhone(), user.getBankName());
            if (accounts != null && !accounts.isEmpty()) {
                accountNumber = accounts.get(0).getAccountNumber();
            }
        }

        model.addAttribute("bank", user.getBankName());
        model.addAttribute("accountNumber", accountNumber);
        model.addAttribute("upiId", upiId);
        return "settings";
    }
//    @PostMapping("/settings/changePin")
//    public ResponseEntity<String> changePin(
//            @RequestBody Map<String, String> request,
//            @AuthenticationPrincipal CustomUserDetails userDetails
//    ) {
//        String currentPin = request.get("currentPin");
//        String newPin = request.get("newPin");
//        String userId = userDetails.getUserId();
//
//        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
//
//        // Validate current PIN
////        if (!passwordEncoder.matches(currentPin, user.getUpiPin())) {
////            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid current PIN");
////        }
//
//        // Update PIN
////        user.setUpiPin(passwordEncoder.encode(newPin));
//        userRepository.save(user);
//
//        return ResponseEntity.ok("PIN changed successfully");
//    }

    @PostMapping("/settings/changePin")
    @ResponseBody
    public String changePin(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody Map<String, String> request) {
        String currentPin = request.get("currentPin");
        String newPin = request.get("newPin");
        User user = userRepository.findById(userDetails.getUserId()).orElseThrow(() -> new RuntimeException("User not found"));
        if (upiService.verifyPin(user.getEmail(), currentPin)) {
            upiService.saveUPIdata(user.getEmail(), newPin , user.getBankName());
            return "Success";
        }
        throw new RuntimeException("Invalid current PIN");
    }

    @GetMapping("/api/searchFriend")
    @ResponseBody
    public Map<String, Boolean> searchFriend(@RequestParam String query) {
        User friend = upiService.searchUser(query);
        return Map.of("found", friend != null);
    }

    @PostMapping("/friends/add")
    public String addFriend(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        upiService.addFriend(userDetails.getUserId(), identifier);
        return "redirect:/dashboard";
    }

    @PostMapping("/family/add")
    public String addFamilyMember(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam String identifier) {
        upiService.addFamilyMember(userDetails.getUserId(), identifier);
        return "redirect:/dashboard";
    }
}
