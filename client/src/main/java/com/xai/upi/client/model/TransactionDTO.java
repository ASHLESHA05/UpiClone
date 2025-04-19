package com.xai.upi.client.model;

import lombok.Data;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
public class TransactionDTO {
    private String type; // "CREDIT" or "DEBIT"
    private String description;
    private Date timestamp;
    private String formattedDate;
    private double amount;
    private String status;

    public TransactionDTO(com.xai.upi.client.model.Transaction tx, String currentUpiId, String senderName, String receiverName) {
        this.amount = tx.getAmount();
        this.status = tx.getStatus();
        this.timestamp = tx.getTimestamp();
        this.formattedDate = formatDate(tx.getTimestamp());

        // Determine transaction type based on current user
        if (currentUpiId.equals(tx.getToUpiId())) {
            this.type = "CREDIT";
            this.description = "Received from " + (senderName != null ? senderName : tx.getFromUserId());
        } else {
            this.type = "DEBIT";
            this.description = "Sent to " + (receiverName != null ? receiverName : tx.getToUpiId());
        }
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat("MMM dd, yyyy hh:mm a").format(date);
    }
}