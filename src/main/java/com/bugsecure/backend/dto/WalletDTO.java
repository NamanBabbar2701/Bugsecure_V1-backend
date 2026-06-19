package com.bugsecure.backend.dto;

import java.util.List;

public class WalletDTO {
    private String walletAddress;
    private Double balance;
    private String currency = "USD";
    private List<TransactionDTO> transactionHistory;

    // Constructors
    public WalletDTO() {
    }

    // Getters and Setters
    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<TransactionDTO> getTransactionHistory() {
        return transactionHistory;
    }

    public void setTransactionHistory(List<TransactionDTO> transactionHistory) {
        this.transactionHistory = transactionHistory;
    }

    // Inner DTO for transactions
    public static class TransactionDTO {
        private String id;
        private String transactionType;
        private Double amount;
        private String currency;
        private String status;
        private String description;
        private String createdAt;
        private String fromUser;
        private String toUser;
        private String withdrawalMethod;
        private String withdrawalReference;
        private String accountHolderName;
        private String ifscCode;

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

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        public String getFromUser() {
            return fromUser;
        }

        public void setFromUser(String fromUser) {
            this.fromUser = fromUser;
        }

        public String getToUser() {
            return toUser;
        }

        public void setToUser(String toUser) {
            this.toUser = toUser;
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
    }
}


