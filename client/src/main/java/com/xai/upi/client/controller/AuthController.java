package com.xai.upi.client.controller;

import com.xai.upi.client.model.User;
import com.xai.upi.client.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/auth/setUpUpiPin")
    public String setUpUpiPin(Model model) {
        User user = getCurrentUser();
        if (user != null) {
            model.addAttribute("user", user);
            return "setUpUpiPin";
        }
        return "redirect:/auth/login";
    }

    @GetMapping("/auth/otpVerification")
    public String otpVerification(Model model) {
        User user = getCurrentUser();
        if (user != null) {
            model.addAttribute("user", user);
            return "otpVerification";
        }
        return "redirect:/auth/login";
    }

    @GetMapping("/auth/cardDetails")
    public String cardDetails(Model model) {
        User user = getCurrentUser();
        if (user != null) {
            model.addAttribute("user", user);
            return "cardDetails";
        }
        return "redirect:/auth/login";
    }

    @GetMapping("/auth/finalUpiPin")
    public String finalUpiPin(Model model) {
        User user = getCurrentUser();
        if (user != null) {
            model.addAttribute("user", user);
            return "finalUpiPin";
        }
        return "redirect:/auth/login";
    }

    private User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            return userRepository.findByUsername(username);
        }
        return null;
    }
}