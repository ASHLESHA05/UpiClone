package com.xai.upi.bank.controller;

import com.xai.upi.bank.model.Account;
import com.xai.upi.bank.model.User;
import com.xai.upi.bank.services.AccountService;
import com.xai.upi.bank.services.CustomUserDetails;
import com.xai.upi.bank.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class BankUIController {
    @Autowired
    private UserService userService;
    @Autowired
    private AccountService accountService;

    @GetMapping("/{bank}")
    public String bankHome(@PathVariable("bank") String bank, Model model) {
        model.addAttribute("bank", bank);
        return "bank_home";
    }


    @GetMapping("/{bank}/signup")
    public String signupForm(@PathVariable("bank") String bank, Model model) {
        model.addAttribute("bank", bank);
        return "signup";
    }


    @GetMapping("/{bank}/login")
    public String loginForm(@PathVariable("bank") String bank, Model model) {
        model.addAttribute("bank", bank);
        return "login";
    }

    @GetMapping("/{bank}/dashboard")
    public String dashboard(@PathVariable("bank") String bank, Model model) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        model.addAttribute("user", userDetails.getUser());
        model.addAttribute("bank", bank);
        return "dashboard";
    }

    @PostMapping("/{bank}/signup")
    public String signup(@PathVariable("bank") String bank, @RequestParam String name, @RequestParam String email,
                         @RequestParam String phone, @RequestParam String password, @RequestParam String aadhar) {
        userService.registerUser(name, email, phone, password, aadhar, bank);
        return "redirect:/" + bank + "/login";
    }



    @GetMapping("/{bank}/accounts")
    @PreAuthorize("#bank == authentication.principal.bank")
    public String accounts(@PathVariable("bank") String bank, Model model) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userDetails.getUser();
        Optional<Account> account = accountService.findByUserIdAndBankName(user.getId(), bank);
        model.addAttribute("bank", bank);
        model.addAttribute("account", account.orElse(null));
        return "accounts";
    }

    @GetMapping("/{bank}/createAccount")
    @PreAuthorize("#bank == authentication.principal.bank")
    public String createAccountForm(@PathVariable("bank") String bank, Model model) {
        model.addAttribute("bank", bank);
        return "create_account";
    }

    @PostMapping("/{bank}/createAccount")
    @PreAuthorize("#bank == authentication.principal.bank")
    public String createAccount(@PathVariable("bank") String bank, @RequestParam double initialAmount) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        accountService.createAccount(bank, userDetails.getUser().getId(), initialAmount);
        return "redirect:/" + bank + "/accounts";
    }

    @GetMapping("/{bank}/profile")
    @PreAuthorize("#bank == authentication.principal.bank")
    public String profile(@PathVariable("bank") String bank, Model model) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        model.addAttribute("user", userDetails.getUser());
        model.addAttribute("bank", bank);
        return "profile";
    }

    @PostMapping("/{bank}/profile")
    @PreAuthorize("#bank == authentication.principal.bank")
    public String updateProfile(@PathVariable("bank") String bank, @RequestParam String name, @RequestParam String phone) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        userService.updateUser(userDetails.getUser().getId(), name, phone);
        return "redirect:/" + bank + "/profile";
    }

    @GetMapping("/{bank}/setPin")
    @PreAuthorize("#bank == authentication.principal.bank")
    public String setPinForm(@PathVariable("bank") String bank, Model model) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<Account> account = accountService.findByUserIdAndBankName(userDetails.getUser().getId(), bank);
        model.addAttribute("account", account.orElse(null));
        model.addAttribute("bank", bank);
        return "set_pin";
    }

    @PostMapping("/{bank}/setPin")
    @PreAuthorize("#bank == authentication.principal.bank")
    public String setPin(@PathVariable("bank") String bank, @RequestParam String pin) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        accountService.setPin(userDetails.getUser().getId(), bank, pin);
        return "redirect:/" + bank + "/accounts";
    }
}