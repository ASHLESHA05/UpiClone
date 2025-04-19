package com.xai.upi.client.controller;

import com.xai.upi.client.model.User;
import com.xai.upi.client.repository.UserRepository;
import com.xai.upi.client.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TransactionControllerTest {

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

        User receiver = new User();
        receiver.setId("user2");
        receiver.setEmail("receiver@example.com");
        receiver.setName("Receiver User");
        receiver.setPhone("9876543210");
        receiver.setBankName("MockBank");
        userRepository.save(receiver);

        userDetails = new CustomUserDetails(user);
    }

    @Test
    public void testTransactionForm() throws Exception {
        mockMvc.perform(get("/transaction/form")
                        .param("receiver", "9876543210@mockbank"))
                .andExpect(status().isOk())
                .andExpect(view().name("transaction-form"));
    }

    @Test
    public void testProcessTransaction() throws Exception {
        mockMvc.perform(post("/transaction/form")
                        .param("receiverPhone", "9876543210")
                        .param("amount", "100.0")
                        .param("upiPin", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/dashboard"));
    }

    @Test
    public void testRequestMoney() throws Exception {
        mockMvc.perform(post("/transaction/request")
                        .param("receiverPhone", "9876543210")
                        .param("amount", "50.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/dashboard"));
    }

    @Test
    public void testNotifications() throws Exception {
        mockMvc.perform(get("/transaction/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications"));
    }

    @Test
    public void testPayRequest() throws Exception {
        mockMvc.perform(get("/transaction/payRequest")
                        .param("id", "notification1")
                        .param("upiId", "9876543210@mockbank")
                        .param("amount", "50.0"))
                .andExpect(status().isOk())
                .andExpect(view().name("pay-request-form"));
    }

    @Test
    public void testProcessPayRequest() throws Exception {
        mockMvc.perform(post("/transaction/payRequest")
                        .param("notificationId", "notification1")
                        .param("receiverPhone", "9876543210")
                        .param("amount", "50.0")
                        .param("upiPin", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/dashboard"));
    }

    @Test
    public void testBankTransfer() throws Exception {
        mockMvc.perform(post("/transaction/bankTransfer")
                        .param("accountNumber", "123456789012")
                        .param("ifscCode", "MOCK0001234")
                        .param("amount", "200.0")
                        .param("upiPin", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/dashboard"));
    }

    @Test
    public void testTransactionHistory() throws Exception {
        mockMvc.perform(get("/transaction/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions"));
    }
}