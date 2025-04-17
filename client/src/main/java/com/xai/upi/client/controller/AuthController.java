package com.xai.upi.client.controller;

import com.xai.upi.client.model.SignUpRequest;
import com.xai.upi.client.model.User;
import com.xai.upi.client.repository.UserRepository;
import com.xai.upi.client.service.UserService;
import com.xai.upi.client.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/signup")
    public String showSignupForm(Model model) {
        model.addAttribute("signupRequest", new SignUpRequest());
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String processSignup(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String aadhar,
            @RequestParam String password,
            @RequestParam String username,
            @RequestParam Integer loginPin,
            Model model
    ) {
        try {
            SignUpRequest signupRequest = new SignUpRequest();
            signupRequest.setName(name);
            signupRequest.setEmail(email);
            signupRequest.setPhone(phone);
            signupRequest.setAadhar(aadhar);
            signupRequest.setPassword(password);
            signupRequest.setUsername(username);
            signupRequest.setLoginPin(loginPin);

            String response = userService.signup(signupRequest);
            if (response.equals("User registered successfully")) {
                // Auto-login after signup
                User user = userRepository.findByEmail(email);
                if (user == null) {
                    model.addAttribute("error", "User not found after signup");
                    return "auth/signup";
                }
                CustomUserDetails userDetails = new CustomUserDetails(
                        user.getId(),
                        email,
                        user.getPassword(),
                        null,
                        null,
                        String.valueOf(loginPin)
                );
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(auth);

                return "redirect:/auth/setUpUpiPin";
            } else {
                model.addAttribute("error", response);
                model.addAttribute("signupRequest", signupRequest);
                return "auth/signup";
            }
        } catch (Exception e) {
            System.out.println("Exception" + e);
            model.addAttribute("error", "Signup failed: " + e.getMessage());
            model.addAttribute("signupRequest", new SignUpRequest());
            return "auth/signup";
        }
    }

    @GetMapping("/login")
    public String showLoginForm(Model model, @RequestParam(value = "error", required = false) String error) {
        if (error != null) {
            model.addAttribute("error", "Invalid email or password");
        }
        return "auth/login";
    }

    @GetMapping("/setUpUpiPin")
    public String setUpUpiPin(Model model) {
        User user = getCurrentUser();
        if (user != null) {
//            model.addAttribute("user", user);
            model.addAttribute("user", user);
            model.addAttribute("bank", "sbi"); // Default bank or fetch from user data
            model.addAttribute("banks", Arrays.asList("sbb", "camera", "united", "axis"));
            // In controller
//            model.addAttribute("accounts", upiService.getAccounts(...)); // returns empty list if none
            return "setUpUpiPin";
        }
        return "redirect:/auth/login";
    }

    @GetMapping("/otpVerification")
    public String otpVerification(Model model) {
        User user = getCurrentUser();
        if (user != null) {
            model.addAttribute("user", user);
            return "otpVerification";
        }
        return "redirect:/auth/login";
    }

    @GetMapping("/cardDetails")
    public String cardDetails(Model model) {
        User user = getCurrentUser();
        if (user != null) {
            model.addAttribute("user", user);
            return "cardDetails";
        }
        return "redirect:/auth/login";
    }

    @GetMapping("/finalUpiPin")
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
            return userRepository.findByEmail(username);
        }
        return null;
    }
}