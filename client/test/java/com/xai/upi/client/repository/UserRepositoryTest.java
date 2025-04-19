package com.xai.upi.client.repository;

import com.xai.upi.client.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
    }

    @Test
    public void testSaveAndFindUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setPhone("1234567890");
        user.setBankName("MockBank");

        User saved = userRepository.save(user);
        assertNotNull(saved.getId());

        User found = userRepository.findByEmail("test@example.com").orElseThrow();
        assertEquals("Test User", found.getName());
    }

    @Test
    public void testFindByUpiId() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setPhone("1234567890");
        user.setBankName("MockBank");
        userRepository.save(user);

        User found = userRepository.findByUpiId("1234567890@mockbank").orElseThrow();
        assertEquals("Test User", found.getName());
    }
}