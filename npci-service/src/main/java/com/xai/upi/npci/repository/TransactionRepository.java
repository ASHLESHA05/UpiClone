package com.xai.upi.npci.repository;

import com.xai.upi.npci.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    List<Transaction> findBySenderUpiIdOrReceiverUpiId(String senderUpiId, String receiverUpiId);
    List<Transaction> findByFromUserIdOrToUpiId(String fromUserId, String toUpiId);
}

