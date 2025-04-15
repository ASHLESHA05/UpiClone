package com.xai.upi.client.controller;

import com.xai.upi.client.model.Account;
import com.xai.upi.client.model.SetUpiPinRequest;
import com.xai.upi.client.service.UPIService;
import com.xai.upi.client.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/{bank}")
public class BankAccountController {

    @Autowired
    private UPIService upiService;

    @GetMapping("/linkBankAccount")
    public String linkBankAccountForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        String userId = userDetails.getUserId();
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
        try {
            // Assuming this method exists in UPIService; if not, you'll need to implement it
            upiService.linkBankAccount(userId, bankName, atmNumber, cvv);
            return "redirect:/" + bankName + "/setUpiPin?userId=" + userId;
        } catch (Exception e) {
            model.addAttribute("error", "Error linking bank account: " + e.getMessage());
            return "linkBankAccount";
        }
    }

    @GetMapping("/get-account-details")
    public String getAccountsByPhoneAndBank(@PathVariable String bank, @RequestParam String phone, @RequestParam String bankName, Model model) {
        List<Account> accounts = upiService.getAccountdetails(phone, bankName);
        model.addAttribute("isNull", accounts == null || accounts.isEmpty());
        if (accounts != null && !accounts.isEmpty()) {
            model.addAttribute("accounts", accounts);
            model.addAttribute("bank", bank);
        }
        return "setUpiPin";
    }

    @PostMapping("/selectedAccount")
    public String saveAccount(@PathVariable String bank, @RequestParam String bankName, @RequestParam String accountNumber,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        String email = userDetails.getEmail();
        upiService.tempSaveaccountData(email, bankName, accountNumber);
        return "otpVerification";
    }

    @GetMapping("/otpVerification")
    public String otpVerify(Model model) {
        String email = CustomUserDetails.getCurrentUserEmail();
        Integer otp = upiService.getotp(email);
        System.out.println("\n\n\n===============OTP FOR : " + email + " is: " + otp + " ===============\n\n");
        model.addAttribute("otp", otp);
        return "otpVerification";
    }

    @PostMapping("/verifyOtp")
    public String verifyOtp(@PathVariable String bank, @RequestParam String otp, Model model,
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

    @GetMapping("/setTpin")
    public String setTpin(@PathVariable String bank, Model model,
                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        String email = userDetails.getEmail();
        List<String> res = upiService.getcardData(email, bank);
        model.addAttribute("CardData", res.get(0));
        model.addAttribute("UpiId", res.get(1));
        return "USetPin";
    }

    @PostMapping("/verifyCard")
    public String verifyCard(@PathVariable String bank, @RequestParam String number, @RequestParam String cvv, Model model,
                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        String email = userDetails.getEmail();
        boolean res = upiService.verifyCard(email, bank, cvv, number);
        if (res) {
            return "redirect:/" + bank + "/setPin";
        } else {
            model.addAttribute("error", "Invalid card details");
            return "USetPin";
        }
    }

    @GetMapping("/setPin")
    public String setPin() {
        return "setPin";
    }

    @PostMapping("/setPin")
    public String setPin(@PathVariable String bank, @RequestParam String cardNumber, @RequestParam String email,
                         @RequestParam Integer pin, @RequestParam String cvv, @RequestParam String upiPin, Model model) {
        boolean saveAllUserdata = upiService.saveUPIdata(email, upiPin);
        if (saveAllUserdata) {
            return "redirect:/" + bank + "/dashboard";
        } else {
            model.addAttribute("error", "Failed to save UPI data");
            return "setPin";
        }
    }
}