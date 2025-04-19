package com.xai.upi.client.controller;

import com.xai.upi.client.dto.SplitBillRequestDTO;
import com.xai.upi.client.model.User;
import com.xai.upi.client.security.CustomUserDetails;
import com.xai.upi.client.service.NotificationService;
import com.xai.upi.client.service.UPIService;
import com.xai.upi.client.service.UserService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import jakarta.validation.Valid; // updated from javax to jakarta

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/transaction")
public class SplitBillController {

    private static final Logger logger = LoggerFactory.getLogger(SplitBillController.class);

    private final UserService userService;
    private final UPIService upiService;
    private final NotificationService notificationService;

    @Autowired
    public SplitBillController(UserService userService,
                               UPIService upiService,
                               NotificationService notificationService) {
        this.userService = userService;
        this.upiService = upiService;
        this.notificationService = notificationService;
    }

    @PostMapping("/splitBill")
    public String splitBill(
            @Valid @ModelAttribute SplitBillRequestDTO request,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        logger.info("Received split bill request: {}", request);

        if (bindingResult.hasErrors()) {
            bindingResult.getAllErrors().forEach(error ->
                    logger.error("Validation error: {}", error));
            model.addAttribute("error", "Invalid request data");
            return "dashboard";
        }

        Double parsedAmount;
        try {
            parsedAmount = Double.parseDouble(request.getAmount());
            if (parsedAmount <= 0) {
                throw new NumberFormatException("Amount must be positive");
            }
        } catch (NumberFormatException e) {
            logger.error("Invalid amount format: {}", request.getAmount());
            model.addAttribute("error", "Invalid amount: " + e.getMessage());
            return "dashboard";
        }

        if (request.getMembers() == null || request.getMembers().isEmpty()) {
            logger.error("No members selected");
            model.addAttribute("error", "Please select at least one member");
            return "dashboard";
        }

        int numMembers = request.getMembers().size() + 1;
        double splitAmount = parsedAmount / numMembers;

        String senderUpiId = userDetails.getUpiId();
        if (senderUpiId == null) {
            senderUpiId = userService.getupiId(userDetails.getEmail());
            if (senderUpiId == null) {
                logger.error("Sender UPI ID not found for email: {}", userDetails.getEmail());
                model.addAttribute("error", "Sender UPI ID not found");
                return "dashboard";
            }
        }

        List<String> errors = new ArrayList<>();
        for (String receiverUpiId : request.getMembers()) {
            User receiver = upiService.searchUser(receiverUpiId);
            if (receiver != null) {
                notificationService.createNotification(
                        receiver.getId(),
                        userDetails.getUserId(),
                        senderUpiId,
                        "Split bill request",
                        splitAmount
                );
                logger.info("Notification sent to receiver: {}", receiverUpiId);
            } else {
                logger.warn("Receiver not found for UPI ID: {}", receiverUpiId);
                errors.add("Receiver not found: " + receiverUpiId);
            }
        }

        if (errors.isEmpty()) {
            model.addAttribute("message", "Split requests sent successfully");
        } else {
            model.addAttribute("error", "Some requests failed: " + String.join(", ", errors));
        }

        return "redirect:/dashboard";
    }
}
