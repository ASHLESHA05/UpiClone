package com.xai.upi.client.repository;

import com.xai.upi.client.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    public void setup() {
        notificationRepository.deleteAll();
    }

    @Test
    public void testSaveAndFindNotification() {
        Notification notification = new Notification();
        notification.setUserId("user1");
        notification.setRequesterUpiId("1234567890@mockbank");
        notification.setRequesterName("Sender User");
        notification.setReceiverUpiId("9876543210@mockbank");
        notification.setReceiverName("Receiver User");
        notification.setAmount(50.0);
        notification.setStatus("PENDING");

        Notification saved = notificationRepository.save(notification);
        assertNotNull(saved.getId());

        List<Notification> notifications = notificationRepository.findByUserIdAndStatus("user1", "PENDING");
        assertEquals(1, notifications.size());
        assertEquals(50.0, notifications.get(0).getAmount());
    }
}