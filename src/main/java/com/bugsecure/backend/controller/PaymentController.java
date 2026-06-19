package com.bugsecure.backend.controller;

import com.bugsecure.backend.dto.PaymentDTO;
import com.bugsecure.backend.model.User;
import com.bugsecure.backend.repository.UserRepository;
import com.bugsecure.backend.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createPayment(
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String bugReportId = body.get("bugReportId");
            String paymentMethod = body.get("paymentMethod");
            
            PaymentDTO created = paymentService.createPayment(bugReportId, paymentMethod, userDetails.getUsername());
            response.put("success", true);
            response.put("data", created);
            response.put("message", "Payment created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/my-payments")
    public ResponseEntity<Map<String, Object>> getMyPayments(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<PaymentDTO> payments;
            if ("COMPANY".equals(user.getRole())) {
                payments = paymentService.getPaymentsByCompany(userDetails.getUsername());
            } else {
                payments = paymentService.getPaymentsByResearcher(userDetails.getUsername());
            }
            
            response.put("success", true);
            response.put("data", payments);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/company")
    public ResponseEntity<Map<String, Object>> getCompanyPayments(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            List<PaymentDTO> payments = paymentService.getPaymentsByCompany(userDetails.getUsername());
            response.put("success", true);
            response.put("data", payments);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/researcher")
    public ResponseEntity<Map<String, Object>> getResearcherPayments(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            List<PaymentDTO> payments = paymentService.getPaymentsByResearcher(userDetails.getUsername());
            response.put("success", true);
            response.put("data", payments);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getPaymentById(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            PaymentDTO payment = paymentService.getPaymentById(id);
            response.put("success", true);
            response.put("data", payment);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updatePaymentStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String status = body.get("status");
            String transactionId = body.get("transactionId");
            String notes = body.get("notes");
            
            PaymentDTO updated = paymentService.updatePaymentStatus(id, status, transactionId, notes, userDetails.getUsername());
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "Payment status updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

