package com.xai.upi.client.config;

import com.xai.upi.client.model.User;
import com.xai.upi.client.repository.UserRepository;
import com.xai.upi.client.security.CustomAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private UserRepository userRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                new AntPathRequestMatcher("/**"),
                                new AntPathRequestMatcher("/auth/signup"),
                                new AntPathRequestMatcher("/auth/login"),
                                new AntPathRequestMatcher("/auth/setUpUpiPin"),
                                new AntPathRequestMatcher("/auth/otpVerification"),
                                new AntPathRequestMatcher("/auth/cardDetails"),
                                new AntPathRequestMatcher("/auth/finalUpiPin"),
                                new AntPathRequestMatcher("/{bank}/**"),
                                new AntPathRequestMatcher("/css/**"),
                                new AntPathRequestMatcher("/js/**"),
                                new AntPathRequestMatcher("/images/**")
                        ).permitAll()
                        .requestMatchers(new AntPathRequestMatcher("/api/ipc/**")).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .successHandler((request, response, authentication) -> {
                            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                            User user = userRepository.findByEmail(userDetails.getUsername());
                            if (user != null && user.isUpiPinSet()) {
                                response.sendRedirect("/dashboard");
                            } else {
                                response.sendRedirect("/auth/setUpUpiPin");
                            }
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/auth/logout"))
                        .logoutSuccessUrl("/auth/login?logout")
                        .permitAll()
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CustomAuthenticationProvider customAuthenticationProvider() {
        return new CustomAuthenticationProvider(userRepository, passwordEncoder());
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.authenticationProvider(customAuthenticationProvider()); // calling the bean method directly
        return authBuilder.build();
    }
}
