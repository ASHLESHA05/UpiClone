package com.xai.upi.client.repository;

import com.xai.upi.client.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUserIdAndStatus(String userId, String status);
    List<Notification> findByRequesterUserIdAndStatus(String requesterUserId, String status);

    @Query("{$or: [{userId: ?0}, {requesterUserId: ?0}], status: ?1}")
    List<Notification> findByUserIdOrRequesterUserIdAndStatus(String userId, String status);
}