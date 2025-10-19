package com.xai.upi.bank.repository.custom.impl;

import com.xai.upi.bank.model.User;
import com.xai.upi.bank.repository.custom.CustomUserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class CustomUserRepositoryImpl implements CustomUserRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<String> findUserIdsByPhone(String phone) {
        TypedQuery<User> query = entityManager.createQuery(
                "SELECT u FROM User u WHERE u.phone = :phone", User.class);
        query.setParameter("phone", phone);
        List<User> users = query.getResultList();
        return users.stream()
                .map(User::getId)
                .collect(Collectors.toList());
    }
}