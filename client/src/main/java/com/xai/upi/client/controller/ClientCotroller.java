package com.xai.upi.client.controller;

import com.xai.upi.client.model.User;
import com.xai.upi.client.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ClientController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/updateUpiStatus")
    public ResponseEntity<String> updateUpiStatus(@RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            user.setIsupiPinSer(true);
            user.setBankName(request.get("bankName")); // Optionally store bankName if needed
            userRepository.save(user);
            return ResponseEntity.ok("UPI status updated");
        }
        return ResponseEntity.badRequest().body("User not found");
    }
}