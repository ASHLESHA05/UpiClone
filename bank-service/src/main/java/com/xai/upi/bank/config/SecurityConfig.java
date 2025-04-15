package com.xai.upi.bank.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import com.xai.upi.bank.filters.FonltHeaderFilter;
import com.xai.upi.bank.services.*;

@EnableWebSecurity
public class SecurityConfig {

    private static final String SECRET_TOKEN = "uyguyfgbsvbcug76t7632$%@^@t";

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

    // Configuration for API endpoints that require header authentication
    @Configuration
    @Order(1)
    public static class ApiSecurityConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .antMatcher("/api/ipc/**")
                    .csrf().disable()
                    .authorizeRequests()
                    .anyRequest().authenticated()
                    .and()
                    .addFilterBefore(new FonltHeaderFilter(), RequestHeaderAuthenticationFilter.class)
                    .addFilter(apiHeaderAuthenticationFilter());
        }

        private RequestHeaderAuthenticationFilter apiHeaderAuthenticationFilter() {
            RequestHeaderAuthenticationFilter filter = new RequestHeaderAuthenticationFilter();
            filter.setPrincipalRequestHeader("X-Internal-Token");
            filter.setExceptionIfHeaderMissing(true);
            filter.setAuthenticationManager(authentication -> {
                String token = (String) authentication.getPrincipal();
                if (SECRET_TOKEN.equals(token)) {
                    authentication.setAuthenticated(true);
                    return authentication;
                } else {
                    throw new org.springframework.security.authentication.BadCredentialsException("Invalid token");
                }
            });
            return filter;
        }
    }

    // Configuration for web pages with form-based authentication
    @Configuration
    @Order(2)
    public static class WebSecurityConfig extends WebSecurityConfigurerAdapter {

        @Autowired
        private UserDetailsService userDetailsService;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private AuthenticationSuccessHandler bankSpecificSuccessHandler;

        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder);
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .csrf().disable()
                    .authorizeRequests()
                    .antMatchers("/", "/{bank}/login", "/{bank}/signup", "/images/**").permitAll()
                    .antMatchers("/", "/{bank}", "/{bank}/login", "/{bank}/signup").permitAll()
                    .antMatchers("/{bank}/dashboard").authenticated()
                    .antMatchers("/{bank}/**").authenticated()
                    .anyRequest().denyAll()
                    .and()
                    .formLogin()
                    .loginPage("/{bank}/login")
                    .loginProcessingUrl("/perform_login")
                    .successHandler(bankSpecificSuccessHandler)
                    .failureUrl("/{bank}/login?error=true")
                    .usernameParameter("username")
                    .passwordParameter("password")
                    .and()
                    .logout()
                    .logoutUrl("/perform_logout")
                    .logoutSuccessUrl("/");
        }
    }
}