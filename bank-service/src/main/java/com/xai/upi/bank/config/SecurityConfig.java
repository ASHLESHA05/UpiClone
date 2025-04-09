package com.xai.upi.bank.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import com.xai.upi.bank.services.   *;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserDetailsService userDetailsService; // Custom service to load users

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // For password hashing
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable() // Disable CSRF for simplicity (enable in production with proper config)
                .authorizeRequests()
                .antMatchers("/", "/{bank}/login", "/{bank}/signup", "/images/**").permitAll() // Public pages
                .antMatchers("/", "/{bank}", "/{bank}/login", "/{bank}/signup").permitAll()
                .antMatchers("/{bank}/dashboard").authenticated() // Protected dashboard
                .antMatchers("/{bank}/**").authenticated()
                .anyRequest().denyAll() // Deny all other requests
                .and()
                .formLogin()
                .loginPage("/{bank}/login") // Bank-specific login page
                .loginProcessingUrl("/perform_login") // URL for form submission
                .successHandler(bankSpecificSuccessHandler()) // Custom redirect logic
                .failureUrl("/{bank}/login?error=true") // Redirect on failure
                .usernameParameter("username") // Hidden field set by JS
                .passwordParameter("password")
                .and()
                .logout()
                .logoutUrl("/perform_logout")
                .logoutSuccessUrl("/");
    }

    @Bean
    public AuthenticationSuccessHandler bankSpecificSuccessHandler() {
        return (request, response, authentication) -> {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String bank = userDetails.getBank(); // Extract bank from authenticated user
            response.sendRedirect("/" + bank + "/dashboard"); // Redirect to bank-specific dashboard
        };
    }
}