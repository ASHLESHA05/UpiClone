package com.xai.upi.client.service;

import com.xai.upi.client.model.Notification;
import com.xai.upi.client.model.NotificationDTO;
import com.xai.upi.client.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public void createNotification(String userId, String requesterUserId, String requesterUpiId, String requesterName, double amount) {
        Notification notification = new Notification();
        notification.setUserId(userId); // Receiver
        notification.setRequesterUserId(requesterUserId); // Sender
        notification.setRequesterUpiId(requesterUpiId);
        notification.setRequesterName(requesterName);
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

    public List<NotificationDTO> getPendingRequestsByUser(String userId) {
        List<Notification> notifications = notificationRepository.findByRequesterUserIdAndStatus(userId, "PENDING");
        return notifications != null ?
                notifications.stream().map(NotificationDTO::new).collect(Collectors.toList()) :
                Collections.emptyList();
    }

    public void approveRequest(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setStatus("COMPLETED");
        notificationRepository.save(notification);
    }

    public void cancelRequest(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setStatus("CANCELLED");
        notificationRepository.save(notification);
    }
}