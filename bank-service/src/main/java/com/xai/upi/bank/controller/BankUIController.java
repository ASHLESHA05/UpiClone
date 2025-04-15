package com.xai.upi.bank.controller;

import com.xai.upi.bank.model.Account;
import com.xai.upi.bank.model.Transaction;
import com.xai.upi.bank.model.User;
import com.xai.upi.bank.services.AccountService;
import com.xai.upi.bank.services.CustomUserDetails;
import com.xai.upi.bank.services.TransactionService;
import com.xai.upi.bank.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class BankUIController {

    private static final Logger logger = LoggerFactory.getLogger(BankUIController.class);
    @Autowired
    private UserService userService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

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
    @PreAuthorize("#bank == authentication.principal.bank")
    public String dashboard(@PathVariable("bank") String bank, Model model) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        User user = userDetails.getUser();
        model.addAttribute("user", user);
        model.addAttribute("bank", bank);

        if ("ADMIN".equals(user.getRole())) {
            List<Account> accounts = accountService.findAllByBankName(bank);
            model.addAttribute("accounts", accounts);

            LocalDate today = LocalDate.now();
            List<Transaction> dailyTransactions = transactionService.findByBankAndDate(bank, today);
            System.out.println("\n\n================Daily Transactions==============\n\n");
            System.out.println(dailyTransactions);
            model.addAttribute("dailyTransactions", dailyTransactions);

            double totalDebits = transactionService.getTotalDebits(bank);
            double totalCredits = transactionService.getTotalCredits(bank);
            System.out.println("Total Debits: " + totalDebits);
            System.out.println("Total Credits: " + totalCredits);

            model.addAttribute("totalDebits", totalDebits);
            model.addAttribute("totalCredits", totalCredits);

            long creditCount = dailyTransactions.stream().filter(t -> "CREDIT".equals(t.getType())).count();
            long debitCount = dailyTransactions.stream().filter(t -> "DEBIT".equals(t.getType())).count();
            model.addAttribute("creditCount", creditCount);
            model.addAttribute("debitCount", debitCount);

            //Dosent get the counts update it ..
            System.out.println("creditCount: " + creditCount);
            System.out.println("debitCount: " + debitCount);

            double totalBalance = accounts.stream().mapToDouble(Account::getBalance).sum();
            model.addAttribute("totalBalance", totalBalance);

            logger.info("Dashboard data - Bank: {}, Daily Transactions: {}, Credit Count: {}, Debit Count: {}, Total Debits: {}, Total Credits: {}, Total Balance: {}",
                    bank, dailyTransactions.size(), creditCount, debitCount, totalDebits, totalCredits, totalBalance);
        } else {
            Optional<Account> account = accountService.findByUserIdAndBankName(user.getId(), bank);
            model.addAttribute("hasAccount", account.isPresent());
        }

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
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        String role = userDetails.getUser().getRole();

        if ("ADMIN".equals(role)) {
            List<Account> accounts = accountService.findAllByBankName(bank);
            model.addAttribute("accounts", accounts);
        } else {
            Optional<Account> account = accountService.findByUserIdAndBankName(userDetails.getUser().getId(), bank);
            model.addAttribute("account", account.orElse(null));
        }

        model.addAttribute("bank", bank);
        model.addAttribute("role", role);
        return "accounts";
    }

    @GetMapping("/{bank}/account/{accountId}")
    @PreAuthorize("#bank == authentication.principal.bank")
    public String accountDetails(@PathVariable("bank") String bank, @PathVariable("accountId") String accountId, Model model) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Account account = accountService.findById(accountId);

        if (!"ADMIN".equals(userDetails.getUser().getRole()) && !account.getUserId().equals(userDetails.getUser().getId())) {
            return "redirect:/" + bank + "/accounts";
        }

        List<Transaction> transactions = transactionService.findByAccountId(accountId);
        model.addAttribute("account", account);
        model.addAttribute("transactions", transactions);
        model.addAttribute("bank", bank);
        model.addAttribute("role", userDetails.getUser().getRole());
        return "account_details";
    }

    @GetMapping("/{bank}/createAccount")
    @PreAuthorize("#bank == authentication.principal.bank")
    public String createAccountForm(@PathVariable("bank") String bank, Model model) {
        model.addAttribute("bank", bank);
        return "create_account";
    }

    @PostMapping("/{bank}/createAccount")
    @PreAuthorize("#bank == authentication.principal.bank")
    public String createAccount(@PathVariable("bank") String bank,
                                @RequestParam double initialAmount,
                                @RequestParam String accountType,
                                @RequestParam(required = false) boolean atmCard) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        accountService.createAccount(bank, userDetails.getUser().getId(), initialAmount, accountType, atmCard);
        return "redirect:/" + bank + "/accounts";
    }

    @GetMapping("/{bank}/generateAtmCard/{accountId}")
    @PreAuthorize("#bank == authentication.principal.bank")
    public String generateAtmCard(@PathVariable("bank") String bank, @PathVariable("accountId") String accountId) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        if (!"ADMIN".equals(userDetails.getUser().getRole())) {
            accountService.generateAtmCard(accountId);
        }
        return "redirect:/" + bank + "/account/" + accountId;
    }

    @GetMapping("/{bank}/profile")
    @PreAuthorize("#bank == authentication.principal.bank")
    public String profile(@PathVariable("bank") String bank, Model model) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        model.addAttribute("user", userDetails.getUser());
        model.addAttribute("bank", bank);
        return "profile";
    }

    @PostMapping("/{bank}/profile")
    @PreAuthorize("#bank == authentication.principal.bank")
    public String updateProfile(@PathVariable("bank") String bank, @RequestParam String name, @RequestParam String phone) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        userService.updateUser(userDetails.getUser().getId(), name, phone);
        return "redirect:/" + bank + "/profile";
    }

    @GetMapping("/{bank}/setPin")
    @PreAuthorize("#bank == authentication.principal.bank")
    public String setPinForm(@PathVariable("bank") String bank, Model model) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Optional<Account> account = accountService.findByUserIdAndBankName(userDetails.getUser().getId(), bank);
        model.addAttribute("account", account.orElse(null));
        model.addAttribute("bank", bank);
        return "set_pin";
    }

    @PostMapping("/{bank}/setPin")
    @PreAuthorize("#bank == authentication.principal.bank")
    public String setPin(@PathVariable("bank") String bank, @RequestParam String pin) {
        CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        accountService.setPin(userDetails.getUser().getId(), bank, pin);
        return "redirect:/" + bank + "/accounts";
    }
}