package com.bugsecure.backend.service;

import com.bugsecure.backend.model.PasswordResetToken;
import com.bugsecure.backend.model.User;
import com.bugsecure.backend.repository.PasswordResetTokenRepository;
import com.bugsecure.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final SecureRandom random = new SecureRandom();

    // Generate OTP
    public Map<String, Object> requestPasswordReset(String email) {
        Map<String, Object> response = new HashMap<>();
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        // Delete existing unused tokens
        Optional<PasswordResetToken> existingToken = tokenRepository.findByUserAndUsedFalse(user);
        existingToken.ifPresent(tokenRepository::delete);

        // Generate OTP (6 digits)
        String otp = String.format("%06d", random.nextInt(999999));
        
        // Generate token
        String token = UUID.randomUUID().toString();

        // Create reset token
        PasswordResetToken resetToken = new PasswordResetToken(token, otp, user);
        tokenRepository.save(resetToken);

        // In production, send OTP via email/SMS
        // For now, return OTP in response (remove in production!)
        response.put("success", true);
        response.put("message", "OTP sent to email (check console/logs in production)");
        response.put("otp", otp); // Remove this in production!
        response.put("token", token);
        
        // TODO: Implement email service to send OTP
        System.out.println("Password Reset OTP for " + email + ": " + otp);
        
        return response;
    }

    // Verify OTP and reset password
    public Map<String, Object> resetPassword(String token, String otp, String newPassword) {
        Map<String, Object> response = new HashMap<>();

        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        if (resetToken.getUsed()) {
            throw new RuntimeException("Reset token has already been used");
        }

        if (resetToken.isExpired()) {
            throw new RuntimeException("Reset token has expired");
        }

        if (!resetToken.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        // Update password
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        response.put("success", true);
        response.put("message", "Password reset successfully");
        return response;
    }

    // Verify OTP only (for frontend validation)
    public Map<String, Object> verifyOtp(String token, String otp) {
        Map<String, Object> response = new HashMap<>();

        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        if (resetToken.getUsed()) {
            throw new RuntimeException("Reset token has already been used");
        }

        if (resetToken.isExpired()) {
            throw new RuntimeException("Reset token has expired");
        }

        if (!resetToken.getOtp().equals(otp)) {
            response.put("success", false);
            response.put("message", "Invalid OTP");
            return response;
        }

        response.put("success", true);
        response.put("message", "OTP verified successfully");
        return response;
    }
}







