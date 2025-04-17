package com.xai.upi.client.controller;

import com.xai.upi.client.model.Account;
import com.xai.upi.client.service.UPIService;
import com.xai.upi.client.service.UserService;
import com.xai.upi.client.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;


import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ArrayList;
@Controller
@RequestMapping("/{bank}")
public class BankAccountController {

    @Autowired
    private UPIService upiService;
    @Autowired
    private UserService userService;

    // Fetch account details based on phone and bank




    // Save selected account and proceed to OTP verification
    @PostMapping("/selectedAccount")
    public String saveAccount(@PathVariable String bank,
                              @RequestParam String bankName,
                              @RequestParam String accountNumber,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        String email = userDetails.getEmail();
        upiService.tempSaveaccountData(email, bankName, accountNumber);
        return "redirect:/" + bank + "/otpVerification";
    }

    // Display OTP verification page
    @GetMapping("/otpVerification")
    public String otpVerify(Model model,@PathVariable String bank, @AuthenticationPrincipal CustomUserDetails userDetails) {
        String email = userDetails.getEmail();
        Integer otp = upiService.getotp(email);
        Map<String,String> userData = userDetails.getFullUserDetails();
        model.addAttribute("otp", otp); // For testing; in production, OTP should be sent via SMS/email
        model.addAttribute("user",userData);
        model.addAttribute("bank",bank);
        return "otpVerification";
    }

    // Verify OTP and proceed to card verification
    @PostMapping("/verifyOtp")
    public String verifyOtp(@PathVariable String bank,
                            @RequestParam String otp,
                            Model model,
                            @AuthenticationPrincipal CustomUserDetails userDetails) {
        String email = userDetails.getEmail();
        boolean result = upiService.verifyOTP(email, otp);
        if (result) {
            return "redirect:/" + bank + "/setTpin";
        } else {
            model.addAttribute("error", "Invalid OTP");
            return "otpVerification";
        }
    }

    // Display card verification page (for ATM card details)
    @GetMapping("/setTpin")
    public String setTpin(@PathVariable String bank,
                          Model model,
                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        String email = userDetails.getEmail();
        List<Map<String, Object>> cardList = upiService.getcardData(email, bank); // already parsed
        model.addAttribute("upiId","<<Yet to Set>>");
        if (!cardList.isEmpty()) {
            model.addAttribute("CardDataList", cardList);

        }
        else{
            model.addAttribute("error","No Cards found on this bank and mobile");
        }

        return "USetPin";
    }


    // Verify card details and proceed to set UPI PIN
    @PostMapping("/verifyCard")
    public String verifyCard(@PathVariable String bank,
                             @RequestParam String number,
                             @RequestParam String cvv,
                             @RequestParam String atmPin,
                             Model model,
                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        String email = userDetails.getEmail();
        boolean res = upiService.verifyCard(email, bank, cvv, number,atmPin);
        if (res) {
            String phone = userService.getPhoneByEmail(email);
            upiService.generateUpiId(bank,email,phone);
            return "redirect:/" + bank + "/setPin";
        } else {
            model.addAttribute("error", "Invalid card details");
            return "USetPin";
        }
    }

    // Display UPI PIN setup page
    @GetMapping("/setPin")
    public String setPin(@PathVariable String bank,
                         Model model,
                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        // Add all required attributes
        model.addAttribute("bankName", bank);  // Changed from "bank" to "bankName"
        model.addAttribute("email", userDetails.getEmail());

        // You need to get masked card number from somewhere (UPIService?)
        String maskedCard = upiService.getMaskedCardNumber(userDetails.getEmail(), bank);
        model.addAttribute("maskedCard", maskedCard);

        String phone = userService.getPhoneByEmail(userDetails.getEmail());
        String upiId = upiService.getUpiId(bank, userDetails.getEmail(), phone);
        model.addAttribute("upiId", upiId);  // Consistent naming (lowercase)

        return "setPin";
    }

    // Save UPI PIN and redirect to dashboard
    @PostMapping("/verifyUpiPin")
    public String setPin(@PathVariable String bank,
                         @RequestParam String upiPin,
                         Model model,
                         @AuthenticationPrincipal CustomUserDetails userDetails) {
        boolean saveAllUserdata = upiService.saveUPIdata(userDetails.getEmail(), upiPin,bank);
        if (saveAllUserdata) {
            return "redirect:/dashboard";
        } else {
            model.addAttribute("error", "Failed to save UPI data wrong Details entered");
            return "setPin";
        }
    }

    // Handle linking bank account (for linkBankAccount.html)
    @GetMapping("/linkBankAccount")
    public String linkBankAccountForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String userId = userDetails.getUserId();
        model.addAttribute("userId", userId);
        List<String> banks = List.of("sbi", "hdfc", "icici", "axis", "pnb", "bob", "canara", "union", "kotak", "yes");
        model.addAttribute("banks", banks);
        return "linkBankAccount";
    }

    @PostMapping("/linkBankAccount")
    public String linkBankAccountSubmit(@RequestParam String userId,
                                        @RequestParam String bankName,
                                        @RequestParam String atmNumber,
                                        @RequestParam String cvv,
                                        Model model) {
        if (!atmNumber.matches("\\d{16}") || !cvv.matches("\\d{3}")) {
            model.addAttribute("error", "Invalid ATM number (16 digits) or CVV (3 digits)");
            return "linkBankAccount";
        }
        try {
            upiService.linkBankAccount(userId, bankName, atmNumber, cvv);
            return "redirect:/" + bankName + "/setUpiPin?userId=" + userId;
        } catch (Exception e) {
            model.addAttribute("error", "Error linking bank account: " + e.getMessage());
            return "linkBankAccount";
        }
    }
}