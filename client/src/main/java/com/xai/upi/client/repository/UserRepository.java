package com.xai.upi.client.repository;

import com.xai.upi.client.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface UserRepository extends MongoRepository<User, String> {
    // Custom query methods
    User findByEmail(String email);
    User findByUsername(String username);
}
