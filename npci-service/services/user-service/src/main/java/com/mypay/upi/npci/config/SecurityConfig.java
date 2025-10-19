package com.mypay.upi.npci.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private static final String INTERNAL_TOKEN = "uyguyfgbsvbcug76t7632$%@^@t";

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .addFilterBefore(requestHeaderAuthenticationFilter(), RequestHeaderAuthenticationFilter.class)
                .authorizeRequests()
                .antMatchers("/api/ipc/**").permitAll()
                .antMatchers("/api/**").permitAll()
                .anyRequest().denyAll();
    }

    private RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter() throws Exception {
        RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
        filter.setPrincipalRequestHeader("X-Internal-Token");
        filter.setCredentialsRequestHeader("X-Internal-Token");
        filter.setExceptionIfHeaderMissing(true);
        filter.setAuthenticationManager(authentication -> {
            String token = (String) authentication.getCredentials();
            if (INTERNAL_TOKEN.equals(token)) {
                authentication.setAuthenticated(true);
            } else {
                throw new org.springframework.security.core.AuthenticationException("Invalid internal token") {};
            }
            return authentication;
        });
        return filter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}