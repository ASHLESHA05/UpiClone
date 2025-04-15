package com.xai.upi.bank.repository.custom.impl;

import com.xai.upi.bank.model.User;
import com.xai.upi.bank.repository.custom.CustomUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class CustomUserRepositoryImpl implements CustomUserRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<String> findUserIdsByPhone(String phone) {
        Query query = new Query(Criteria.where("phone").is(phone));
        query.fields().include("_id"); // Only include the ID field

        List<User> users = mongoTemplate.find(query, User.class);
        return users.stream()
                .map(User::getId) // Make sure User has a getId() method
                .collect(Collectors.toList());
    }
}
