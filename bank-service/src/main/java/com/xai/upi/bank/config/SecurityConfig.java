// src/main/java/com/xai/upi/bank/config/SecurityConfig.java
package com.xai.upi.bank.config;

import com.xai.upi.bank.filters.FonltHeaderFilter;
import com.xai.upi.bank.services.CustomUserDetails;
import com.xai.upi.bank.services.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${upi.secret-token:${SECRET_TOKEN:}}")
    private String secretToken;

    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationSuccessHandler bankSpecificSuccessHandler() {
        return (request, response, authentication) -> {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String bank = userDetails.getBank();
            response.sendRedirect("/" + bank + "/dashboard");
        };
    }

    @Bean
    public AuthenticationFailureHandler bankSpecificFailureHandler() {
        return (request, response, exception) -> {
            String username = request.getParameter("username");
            String bank = "";
            if (username != null && username.contains(":")) {
                bank = username.split(":")[0];
            } else {
                // Fallback: extract from referer if possible
                String referer = request.getHeader("Referer");
                if (referer != null && referer.contains("/")) {
                    String[] parts = referer.split("/");
                    if (parts.length > 1) {
                        bank = parts[1];
                    }
                }
            }
            if (bank.isEmpty()) {
                bank = "default"; // Fallback to landing
                response.sendRedirect("/?error=true");
            } else {
                response.sendRedirect("/" + bank + "/login?error=true");
            }
        };
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userDetailsService);
        p.setPasswordEncoder(passwordEncoder());
        return p;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/ipc/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterBefore(fonltHeaderFilter(), RequestHeaderAuthenticationFilter.class)
                .addFilter(apiHeaderAuthenticationFilter());
        return http.build();
    }

    @Bean
    public FonltHeaderFilter fonltHeaderFilter() {
        return new FonltHeaderFilter();
    }

    private RequestHeaderAuthenticationFilter apiHeaderAuthenticationFilter() {
        RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
        filter.setPrincipalRequestHeader("X-Internal-Token");
        filter.setExceptionIfHeaderMissing(true);

        AuthenticationManager authManager = authentication -> {
            Object principal = authentication.getPrincipal();
            String token = principal != null ? principal.toString() : null;
            if (secretToken != null && secretToken.equals(token)) {
                authentication.setAuthenticated(true);
                return authentication;
            }
            throw new BadCredentialsException("Invalid token");
        };

        filter.setAuthenticationManager(authManager);
        return filter;
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http,
                                                      AuthenticationSuccessHandler bankSpecificSuccessHandler,
                                                      AuthenticationFailureHandler bankSpecificFailureHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/{bank}/login" ,"/{bank}/signup","/login", "/signup", "/images/**", "/css/**", "/js/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/signup").permitAll()
                        .requestMatchers("/{bank}/**").permitAll()
                        .anyRequest().denyAll()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform_login")
                        .successHandler(bankSpecificSuccessHandler)
                        .failureHandler(bankSpecificFailureHandler)
                        .usernameParameter("username")
                        .passwordParameter("password")
                )
                .logout(logout -> logout
                        .logoutUrl("/perform_logout")
                        .logoutSuccessUrl("/")
                );
        return http.build();
    }
}