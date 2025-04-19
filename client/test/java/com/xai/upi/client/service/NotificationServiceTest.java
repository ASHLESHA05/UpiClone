package com.xai.upi.client.service;

import com.xai.upi.client.model.Notification;
import com.xai.upi.client.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    public void setup() {
        notificationRepository.deleteAll();
    }

    @Test
    public void testSaveNotification() {
        Notification notification = new Notification();
        notification.setUserId("user1");
        notification.setRequesterUpiId("1234567890@mockbank");
        notification.setRequesterName("Sender User");
        notification.setReceiverUpiId("9876543210@mockbank");
        notification.setReceiverName("Receiver User");
        notification.setAmount(50.0);
        notification.setStatus("PENDING");

        Notification saved = notificationService.saveNotification(notification);
        assertNotNull(saved.getId());
        assertEquals("PENDING", saved.getStatus());
    }

    @Test
    public void testGetPendingNotifications() {
        Notification notification = new Notification();
        notification.setUserId("user1");
        notification.setRequesterUpiId("1234567890@mockbank");
        notification.setRequesterName("Sender User");
        notification.setReceiverUpiId("9876543210@mockbank");
        notification.setReceiverName("Receiver User");
        notification.setAmount(50.0);
        notification.setStatus("PENDING");
        notificationRepository.save(notification);

        List<Notification> notifications = notificationService.getPendingNotifications("user1");
        assertEquals(1, notifications.size());
        assertEquals("Sender User", notifications.get(0).getRequesterName());
    }

    @Test
    public void testCompleteNotification() {
        Notification notification = new Notification();
        notification.setUserId("user1");
        notification.setRequesterUpiId("1234567890@mockbank");
        notification.setRequesterName("Sender User");
        notification.setReceiverUpiId("9876543210@mockbank");
        notification.setReceiverName("Receiver User");
        notification.setAmount(50.0);
        notification.setStatus("PENDING");
        Notification saved = notificationRepository.save(notification);

        notificationService.completeNotification(saved.getId());
        Notification updated = notificationRepository.findById(saved.getId()).orElseThrow();
        assertEquals("COMPLETED", updated.getStatus());
    }
}