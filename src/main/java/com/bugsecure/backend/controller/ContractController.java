package com.bugsecure.backend.controller;

import com.bugsecure.backend.model.User;
import com.bugsecure.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/contract")
@CrossOrigin(origins = "*")
public class ContractController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/accept")
    public ResponseEntity<Map<String, Object>> acceptContract(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Generate contract hash
            String contractText = body.get("contractText");
            String contractHash = generateContractHash(user.getEmail(), contractText, LocalDateTime.now().toString());

            user.setContractAccepted(true);
            user.setContractHash(contractHash);
            user.setContractAcceptedAt(LocalDateTime.now());

            user = userRepository.save(user);

            response.put("success", true);
            response.put("message", "Contract accepted successfully");
            response.put("contractHash", contractHash);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getContractStatus(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            response.put("success", true);
            response.put("contractAccepted", user.getContractAccepted() != null ? user.getContractAccepted() : false);
            response.put("contractHash", user.getContractHash());
            response.put("contractAcceptedAt", user.getContractAcceptedAt());
            response.put("companyAgreementAccepted", user.getCompanyAgreementAccepted() != null ? user.getCompanyAgreementAccepted() : false);
            response.put("companyAgreementHash", user.getCompanyAgreementHash());
            response.put("companyAgreementSignedOn", user.getCompanyAgreementSignedOn());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/company/accept")
    public ResponseEntity<Map<String, Object>> acceptCompanyAgreement(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Only companies can accept company agreement
            if (!"COMPANY".equals(user.getRole())) {
                throw new RuntimeException("Only companies can accept the company agreement");
            }

            // Generate agreement hash
            String agreementText = body.get("agreementText");
            String agreementHash = generateContractHash(user.getEmail(), agreementText, LocalDateTime.now().toString());

            user.setCompanyAgreementAccepted(true);
            user.setCompanyAgreementHash(agreementHash);
            user.setCompanyAgreementSignedOn(LocalDateTime.now());

            user = userRepository.save(user);

            response.put("success", true);
            response.put("message", "Company agreement accepted successfully");
            response.put("agreementHash", agreementHash);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/company/status")
    public ResponseEntity<Map<String, Object>> getCompanyAgreementStatus(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            response.put("success", true);
            response.put("companyAgreementAccepted", user.getCompanyAgreementAccepted() != null ? user.getCompanyAgreementAccepted() : false);
            response.put("companyAgreementHash", user.getCompanyAgreementHash());
            response.put("companyAgreementSignedOn", user.getCompanyAgreementSignedOn());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private String generateContractHash(String email, String contractText, String timestamp) {
        try {
            String data = email + contractText + timestamp;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return "CONTRACT_" + hexString.toString().substring(0, 16).toUpperCase();
        } catch (Exception e) {
            return "CONTRACT_" + System.currentTimeMillis();
        }
    }
}


