package com.xai.upi.client.repository;

import com.xai.upi.client.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUserIdAndStatus(String userId, String status);
}