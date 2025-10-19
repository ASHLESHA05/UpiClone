package com.xai.upi.bank.services;

import com.xai.upi.bank.model.User;
import com.xai.upi.bank.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(String name, String email, String phone, String password, String aadhar, String bank) {
        long userCount = userRepository.countByBank(bank);
        String role = userCount == 0 ? "ADMIN" : "USER";
        User user = new User();
        user.setName(name);
        user.setEmail(email.toLowerCase());
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(password));
        user.setAadhar(aadhar);
        user.setBank(bank.toLowerCase());
        user.setRole(role);
        return userRepository.save(user);
    }

    public User updateUser(String id, String name, String phone) {
        User user = findById(id);
        user.setName(name);
        user.setPhone(phone);
        return userRepository.save(user);
    }

    public List<String> findUsersId(String phone) {
        return userRepository.findUserIdsByPhone(phone); // ✅ Correct
    }

    public Optional<User> finduserByEmailBank(String email,String BankName){
        return userRepository.findByEmailAndBank(email.toLowerCase(), BankName.toLowerCase());
    }

    public User findById(String id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void updateRole(String id, String role) {
        User user = findById(id);
        user.setRole(role);
        userRepository.save(user);
    }
}