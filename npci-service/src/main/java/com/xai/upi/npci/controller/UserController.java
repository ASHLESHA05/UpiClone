package com.xai.upi.npci.controller;

import com.xai.upi.npci.model.User;
import com.xai.upi.npci.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/user")
    public Map<String, Object> getUserByUserId(@RequestParam String userId) {
        Map<String, Object> response = new HashMap<>();
        System.out.println("Fetching from DB: " + userRepository.findByUserId(userId));

        Optional<User> optionalUser = userRepository.findByUserId(userId);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            response.put("userId", user.getUserId());
            response.put("name", user.getName());
            response.put("email", user.getEmail());
            response.put("phone", user.getPhone());
            response.put("aadhar", user.getAadhar());
            response.put("upiId", user.getUpiId());
            response.put("bankAccountId", user.getBankAccountId());
            response.put("bankName", user.getBankName());
            response.put("friends", user.getFriends());
            response.put("familyMembers", user.getFamilyMembers());
            return response;
        } else {
            throw new RuntimeException("User with userId " + userId + " not found.");
        }
    }
}
