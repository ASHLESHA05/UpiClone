package com.xai.upi.bank.services;

import com.xai.upi.bank.model.User;
import com.xai.upi.bank.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String[] parts = username.split(":", 2); // Expecting "bank:email"
        if (parts.length != 2) {
            throw new UsernameNotFoundException("Invalid username format. Use bank:email");
        }
        String bank = parts[0].toLowerCase(); // Normalize to lowercase
        String email = parts[1].toLowerCase(); // Normalize to lowercase
        User user = userRepository.findByEmailAndBank(email, bank)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return new CustomUserDetails(user); // Wrap user in CustomUserDetails
    }
}