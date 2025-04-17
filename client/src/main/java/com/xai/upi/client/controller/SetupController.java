package com.xai.upi.client.controller;

import com.xai.upi.client.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SetupController {

    // Display initial phone number entry form after signup
    @GetMapping("/setup-upi")
    public String setupUpiForm(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
        model.addAttribute("user", userDetails); // Provides user details (e.g., phone, id) to setUpipin.html
        return "setUpipin"; // Renders setUpipin.html for phone number and bank selection
    }
}