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
public class UserService implements UserStatusService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String INTERNAL_TOKEN = "uyguyfgbsvbcug76t7632$%@^@t";
    private static final String BASE_URL = "http://localhost:8082/api";

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Internal-Token", INTERNAL_TOKEN);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
    @Override
    public void markUpiPinAsSet(String email,String bankName) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            user.setUpiPinSet(true);
            user.setBankName(bankName);
            userRepository.save(user);
        }
    }

    public String getupiId(String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            return user.getUpiId();
        }
        return null;
    }

    public String signup(SignUpRequest signupRequest) {
        // Map SignUpRequest to NPCI's expected fields
        String usrId = UUID.randomUUID().toString();
        Map<String, String> requestBody = Map.of(
                "name", signupRequest.getName(),
                "email", signupRequest.getEmail(),
                "phone", signupRequest.getPhone(),
                "password", signupRequest.getPassword(),
                "userId",usrId
        );

        // Call NPCI service
        System.out.println("Calling NPCI for signUP");
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, getHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                BASE_URL + "/ipc/signup",
                HttpMethod.POST,
                entity,
                  String.class
        );
        System.out.println(response.getBody());

        if (!response.getBody().equals("User registered successfully")) {
            return response.getBody();
        }

        // Save user locally
        User user = new User();
        user.setId(usrId);
        user.setName(signupRequest.getName());
        user.setPhone(signupRequest.getPhone());
        user.setEmail(signupRequest.getEmail());
        user.setAadhar(signupRequest.getAadhar());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setUsername(signupRequest.getUsername());
        user.setLoginPin(signupRequest.getLoginPin());
        user.setUpiPinSet(false);
        userRepository.save(user);

        return response.getBody();
    }

    public void linkBankAccount(String userId, String bankName, String atmNumber, String cvv) {
        Map<String, String> requestBody = Map.of(
                "userId", userId,
                "bankName", bankName,
                "atmNumber", atmNumber,
                "cvv", cvv
        );
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, getHeaders());
        restTemplate.exchange(
                BASE_URL + "/ipc/linkBankAccount",
                HttpMethod.POST,
                entity,
                Void.class
        );
    }

    public void setUpiPin(String email, String upiPin,String bankName) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            user.setUpiPinSet(true);
            userRepository.save(user);
        }
    }
    public Map<String, Object> getUserData(String userId) {
        HttpEntity<String> entity = new HttpEntity<>(getHeaders());
        System.out.println("Calling NPCI for getUserData"+userId);
        ResponseEntity<Map> response = restTemplate.exchange(BASE_URL + "/user?userId=" + userId, HttpMethod.GET, entity, Map.class);
        System.out.println("=================\nFetched User Data\n===============\n");
        return response.getBody();
    }


    public String getPhoneByEmail(String email) {
        System.out.println("getPhoneByEmail");
        User usr = userRepository.findByEmail(email);
        System.out.println("SER"+usr);
        if (usr != null) {
            return usr.getPhone();
        }
        return null;  // Return null if user is not found
    }
    public User findUserByEmail(String email){
        System.out.println("findUserByEmail");
        User user = userRepository.findByEmail(email);
        if(user != null) {
            return user;
        }
        return null;
    }

}