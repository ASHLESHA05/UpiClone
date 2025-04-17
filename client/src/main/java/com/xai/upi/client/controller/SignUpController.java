package com.xai.upi.client.controller;

import com.xai.upi.client.model.Account;
import com.xai.upi.client.service.UPIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
public class SignUpController {

    @Autowired
    private UPIService upiService;

    @GetMapping("/get-account-details")
    public String getAccountsByPhoneAndBank(@RequestParam String phone,
                                            @RequestParam String bankName,
                                            Model model) {
        System.out.println("Fetching accounts for bank: " + bankName + ", phone: " + phone);

        List<Account> accounts = upiService.getAccountdetails(phone, bankName);
        model.addAttribute("bank", bankName);

        if (accounts == null || accounts.isEmpty()) {
            model.addAttribute("accounts", new ArrayList<>());
            model.addAttribute("error", "No bank accounts found for the provided phone number and bank.");
        } else {
            model.addAttribute("accounts", accounts);
        }

        return "setUpUpiPin";
    }
}
