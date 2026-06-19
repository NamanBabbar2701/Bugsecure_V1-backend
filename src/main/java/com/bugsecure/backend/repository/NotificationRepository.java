package com.bugsecure.backend.repository;

import com.bugsecure.backend.model.Notification;
import com.bugsecure.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {
    Page<Notification> findByRecipientAndRead(User recipient, Boolean read, Pageable pageable);
    Page<Notification> findByRecipientAndReadOrderByCreatedAtDesc(User recipient, Boolean read, Pageable pageable);
    Page<Notification> findByRecipient(User recipient, Pageable pageable);
    Page<Notification> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);
    int countByRecipientAndRead(User recipient, Boolean read);
    List<Notification> findTop10ByRecipientAndReadOrderByCreatedAtDesc(User recipient, Boolean read);
}

