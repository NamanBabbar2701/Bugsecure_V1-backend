package com.bugsecure.backend.repository;

import com.bugsecure.backend.model.PasswordResetToken;
import com.bugsecure.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends MongoRepository<PasswordResetToken, String> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByOtp(String otp);
    Optional<PasswordResetToken> findByUserAndUsedFalse(User user);
    void deleteByUser(User user);
}







