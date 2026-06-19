package com.bugsecure.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.LocalDateTime;

@Document(collection = "wallet_transactions")
public class WalletTransaction {

    @Id
    private String id;
    private String transactionType; // DEPOSIT, WITHDRAWAL, TRANSFER, REWARD
    private Double amount;
    private String currency = "USD"; // USD, INR, etc.
    private String status = "PENDING"; // PENDING, COMPLETED, FAILED
    // Canonical amount in USD for ledger consistency (optional, used for multi-currency requests)
    private Double amountUsd;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @DBRef
    private User user; // Owner of the wallet
    
    @DBRef
    private User fromUser; // For transfers (optional)
    
    @DBRef
    private User toUser; // For transfers (optional)
    
    @DBRef
    private Payment payment; // Link to payment if reward-based (optional)
    
    private String transactionHash; // Unique transaction identifier
    
    // Withdrawal details
    private String withdrawalMethod; // BANK_TRANSFER, UPI, PAYPAL
    private String withdrawalReference; // Account No, IFSC, UPI ID, etc.
    private String accountHolderName; // For bank transfers
    private String ifscCode; // For bank transfers

    // Constructors
    public WalletTransaction() {
    }

    public WalletTransaction(String transactionType, Double amount, User user) {
        this.transactionType = transactionType;
        this.amount = amount;
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getAmountUsd() {
        return amountUsd;
    }

    public void setAmountUsd(Double amountUsd) {
        this.amountUsd = amountUsd;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getFromUser() {
        return fromUser;
    }

    public void setFromUser(User fromUser) {
        this.fromUser = fromUser;
    }

    public User getToUser() {
        return toUser;
    }

    public void setToUser(User toUser) {
        this.toUser = toUser;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public String getWithdrawalMethod() {
        return withdrawalMethod;
    }

    public void setWithdrawalMethod(String withdrawalMethod) {
        this.withdrawalMethod = withdrawalMethod;
    }

    public String getWithdrawalReference() {
        return withdrawalReference;
    }

    public void setWithdrawalReference(String withdrawalReference) {
        this.withdrawalReference = withdrawalReference;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }
    
    public void setCreatedAtIfNew() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}


