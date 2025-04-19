package com.xai.upi.client.service;

import com.xai.upi.client.model.Notification;
import com.xai.upi.client.model.Transaction;
import com.xai.upi.client.model.User;
import com.xai.upi.client.repository.TransactionRepository;
import com.xai.upi.client.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class UPIServiceTest {

    @Autowired
    private UPIService upiService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
        transactionRepository.deleteAll();

        User sender = new User();
        sender.setId("user1");
        sender.setEmail("sender@example.com");
        sender.setName("Sender User");
        sender.setPhone("1234567890");
        sender.setBankName("MockBank");
        userRepository.save(sender);

        User receiver = new User();
        receiver.setId("user2");
        receiver.setEmail("receiver@example.com");
        receiver.setName("Receiver User");
        receiver.setPhone("9876543210");
        receiver.setBankName("MockBank");
        userRepository.save(receiver);
    }

    @Test
    public void testGetUpiId() {
        String upiId = upiService.getUpiId("MockBank", "sender@example.com", "1234567890");
        assertEquals("1234567890@mockbank", upiId);
    }

    @Test
    public void testProcessTransaction() {
        Transaction transaction = upiService.processTransaction(
                "1234567890@mockbank",
                "9876543210@mockbank",
                100.0,
                "123456"
        );
        assertNotNull(transaction);
        assertEquals("SUCCESS", transaction.getStatus());
        assertEquals(100.0, transaction.getAmount());
    }

    @Test
    public void testRequestMoney() {
        Notification notification = upiService.requestMoney(
                "1234567890@mockbank",
                "9876543210@mockbank",
                50.0
        );
        assertNotNull(notification);
        assertEquals("PENDING", notification.getStatus());
        assertEquals("Sender User", notification.getRequesterName());
        assertEquals("Receiver User", notification.getReceiverName());
    }

    @Test
    public void testProcessBankTransfer() {
        Transaction transaction = upiService.processBankTransfer(
                "1234567890@mockbank",
                "123456789012",
                "MOCK0001234",
                200.0,
                "123456"
        );
        assertNotNull(transaction);
        assertEquals("SUCCESS", transaction.getStatus());
        assertEquals(200.0, transaction.getAmount());
    }

    @Test
    public void testGenerateQrCode() {
        String qrCode = upiService.generateQrCode("1234567890@mockbank");
        assertNotNull(qrCode);
        assertTrue(qrCode.length() > 0);
    }
}