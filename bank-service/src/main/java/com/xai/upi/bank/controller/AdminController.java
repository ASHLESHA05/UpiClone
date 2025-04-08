package com.xai.upi.bank.controller;

import com.xai.upi.bank.model.Account;
import com.xai.upi.bank.services.AccountService;
import com.xai.upi.bank.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/{bank}/admin")
@PreAuthorize("hasRole('ADMIN') and #bank == authentication.principal.bank")
public class AdminController {
    @Autowired
    private UserService userService;
    @Autowired
    private AccountService accountService;

    @GetMapping("/dashboard")
    public String adminDashboard(@PathVariable("bank") String bank, Model model) {
        List<Account> accounts = accountService.findAllByBankName(bank);
        model.addAttribute("accounts", accounts);
        model.addAttribute("bank", bank);
        return "admin_dashboard";
    }

    @GetMapping("/addEmployee")
    public String addEmployeeForm(@PathVariable("bank") String bank, Model model) {
        model.addAttribute("bank", bank);
        return "add_employee";
    }

    @PostMapping("/addEmployee")
    public String addEmployee(@PathVariable("bank") String bank, @RequestParam String name, @RequestParam String email,
                              @RequestParam String phone, @RequestParam String password) {
        userService.registerUser(name, email, phone, password, null, bank).setRole("EMPLOYEE");
        return "redirect:/" + bank + "/admin/dashboard";
    }
}