package com.bugsecure.backend.repository;

import com.bugsecure.backend.model.User;
import com.bugsecure.backend.model.WalletTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface WalletTransactionRepository extends MongoRepository<WalletTransaction, String> {
    List<WalletTransaction> findByUser(User user);
    List<WalletTransaction> findByUserOrderByCreatedAtDesc(User user);
    List<WalletTransaction> findByStatus(String status);
    List<WalletTransaction> findByTransactionType(String transactionType);
    Optional<WalletTransaction> findByTransactionHash(String transactionHash);
}







