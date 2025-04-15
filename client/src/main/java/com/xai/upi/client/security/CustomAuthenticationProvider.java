package com.xai.upi.client.security;

import com.xai.upi.client.model.User;
import com.xai.upi.client.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;

public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final UserRepository userRepository; // Injected via constructor
    private final PasswordEncoder passwordEncoder; // Injected via constructor

    // Constructor for dependency injection
    public CustomAuthenticationProvider(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String identifier = authentication.getName(); // Email or username
        String credential = (String) authentication.getCredentials(); // Password or PIN

        User user;
        if (identifier.contains("@")) {
            // Treat as email and verify password
            user = userRepository.findByEmail(identifier);
            if (user == null || !passwordEncoder.matches(credential, user.getPassword())) {
                throw new BadCredentialsException("Invalid email or password");
            }
        } else {
            // Treat as username and verify PIN
            user = userRepository.findByUsername(identifier);
            if (user == null || !String.valueOf(user.getLoginPin()).equals(credential)) {
                throw new BadCredentialsException("Invalid username or PIN");
            }
        }

        // Create UserDetails with minimal data
        CustomUserDetails userDetails = new CustomUserDetails(
                user.getId(), // Assuming User has an ID field
                user.getEmail(),
                user.getPassword(),
                null, // upiId (not stored locally yet)
                null, // bankAccountId (not stored locally yet)
                String.valueOf(user.getLoginPin())
        );

        return new UsernamePasswordAuthenticationToken(userDetails, credential, userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}