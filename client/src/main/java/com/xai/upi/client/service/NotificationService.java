package com.xai.upi.client.service;

import com.xai.upi.client.model.Notification;
import com.xai.upi.client.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public void createNotification(String userId, String requesterUpiId, double amount) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setRequesterUpiId(requesterUpiId);
        notification.setAmount(amount);
        notification.setStatus("PENDING");
        notificationRepository.save(notification);
    }

    public List<Notification> getPendingNotifications(String userId) {
        return notificationRepository.findByUserIdAndStatus(userId, "PENDING");
    }

    public void completeNotification(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setStatus("COMPLETED");
        notificationRepository.save(notification);
    }
}