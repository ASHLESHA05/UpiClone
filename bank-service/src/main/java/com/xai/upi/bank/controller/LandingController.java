package com.xai.upi.bank.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LandingController {
    @GetMapping("/")
    public String landing() {
        return "landing";
    }

    @GetMapping("/bank")
    public String redirectToBank(@RequestParam("bank") String bank) {
        return "redirect:/" + bank;
    }
}
