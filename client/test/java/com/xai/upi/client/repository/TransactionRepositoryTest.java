package com.xai.upi.client.repository;

import com.xai.upi.client.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    public void setup() {
        transactionRepository.deleteAll();
    }

    @Test
    public void testSaveAndFindTransaction() {
        Transaction transaction = new Transaction();
        transaction.setSenderUpiId("1234567890@mockbank");
        transaction.setReceiverUpiId("9876543210@mockbank");
        transaction.setAmount(100.0);
        transaction.setStatus("SUCCESS");
        transaction.setTimestamp(new Date());

        Transaction saved = transactionRepository.save(transaction);
        assertNotNull(saved.getId());

        List<Transaction> transactions = transactionRepository.findBySenderUpiIdOrReceiverUpiId(
                "1234567890@mockbank", "9876543210@mockbank");
        assertEquals(1, transactions.size());
        assertEquals(100.0, transactions.get(0).getAmount());
    }
}