package com.xai.upi.client.controller;

import com.xai.upi.client.model.User;
import com.xai.upi.client.repository.UserRepository;
import com.xai.upi.client.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private CustomUserDetails userDetails;

    @BeforeEach
    public void setup() {
        User user = new User();
        user.setId("user1");
        user.setEmail("test@example.com");
        user.setName("Test User");
        user.setPhone("1234567890");
        user.setBankName("MockBank");
        userRepository.save(user);

        userDetails = new CustomUserDetails(user);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    public void testDashboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    public void testProfile() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    public void testUpdateProfile() throws Exception {
        mockMvc.perform(post("/profile/update")
                        .param("name", "Updated User")
                        .param("phone", "9876543210"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/profile"));

        User updatedUser = userRepository.findByEmail("test@example.com").orElseThrow();
        assert updatedUser.getName().equals("Updated User");
        assert updatedUser.getPhone().equals("9876543210");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    public void testSettings() throws Exception {
        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    public void testChangePin() throws Exception {
        mockMvc.perform(post("/settings/changePin")
                        .contentType("application/json")
                        .content("{\"currentPin\":\"123456\",\"newPin\":\"654321\"}"))
                .andExpect(status().isOk());
    }
}