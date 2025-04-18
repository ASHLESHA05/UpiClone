package com.xai.upi.client.model;

import lombok.Data;
import java.util.Date;
import java.text.SimpleDateFormat;


@Data
public class TransactionDTO {
    private String type; // "CREDIT" or "DEBIT"
    private String description;
    private Date timestamp;
    private String formattedDate;
    private double amount;
    private String status;

    // Constructor to convert from server Transaction
    public TransactionDTO(com.xai.upi.client.model.Transaction tx, String currentUpiId) {
        this.amount = tx.getAmount();
        this.status = tx.getStatus();
        this.timestamp = tx.getTimestamp();
        this.formattedDate = formatDate(tx.getTimestamp());

        // Determine transaction type based on current user
        if(currentUpiId.equals(tx.getReceiverUpiId())) {
            this.type = "CREDIT";
            this.description = "Received from " + tx.getSenderUpiId();
        } else {
            this.type = "DEBIT";
            this.description = "Sent to " + tx.getReceiverUpiId();
        }
    }

    private String formatDate(Date date) {
        // Implement your date formatting logic
        return new SimpleDateFormat("MMM dd, yyyy hh:mm a").format(date);
    }
}