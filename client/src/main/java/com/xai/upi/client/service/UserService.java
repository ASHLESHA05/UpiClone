package com.xai.upi.client.service;

import com.xai.upi.client.model.SignUpRequest;
import com.xai.upi.client.model.User;
import com.xai.upi.client.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String INTERNAL_TOKEN = "uyguyfgbsvbcug76t7632$%@^@t";
    private static final String BASE_URL = "http://localhost:8082/api/ipc";

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        return headers;
    }

    public Map<String, String> signup(SignUpRequest signupRequest) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setUsername(signupRequest.getUsername());
        user.setPhone(signupRequest.getPhone());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setLoginPin(signupRequest.getLoginPin());
        user.setEmail(signupRequest.getEmail());
        userRepository.save(user);

        HttpEntity<SignUpRequest> entity = new HttpEntity<>(signupRequest, getHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(BASE_URL + "/signup", HttpMethod.POST, entity, Map.class);
        return response.getBody();
    }

    public Map<String, Object> getUserByEmail(String email) {
        HttpEntity<String> entity = new HttpEntity<>(getHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(
                BASE_URL + "/userByEmail?email=" + email, HttpMethod.GET, entity, Map.class);
        return response.getBody();
    }

    public void linkBankAccount(String userId, String bankName, String atmNumber, String cvv) {
        Map<String, String> requestBody = Map.of("userId", userId, "bankName", bankName, "atmNumber", atmNumber, "cvv", cvv);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, getHeaders());
        restTemplate.exchange(BASE_URL + "/linkBankAccount", HttpMethod.POST, entity, Map.class);
    }

    public Map<String, Object> getUserData(String userId) {
        HttpEntity<String> entity = new HttpEntity<>(getHeaders());
        ResponseEntity<Map> response = restTemplate.exchange(BASE_URL + "/user?userId=" + userId, HttpMethod.GET, entity, Map.class);
        return response.getBody();
    }
}
