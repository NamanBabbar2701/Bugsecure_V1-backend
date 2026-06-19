package com.bugsecure.backend.controller;

import com.bugsecure.backend.dto.WalletDTO;
import com.bugsecure.backend.model.User;
import com.bugsecure.backend.repository.UserRepository;
import com.bugsecure.backend.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@CrossOrigin(origins = "*")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getWallet(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            WalletDTO wallet = walletService.getWallet(userDetails.getUsername());
            response.put("success", true);
            response.put("data", wallet);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/balance")
    public ResponseEntity<Map<String, Object>> getBalance(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Double balance = walletService.getBalance(userDetails.getUsername());
            response.put("success", true);
            response.put("balance", balance);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/deposit")
    public ResponseEntity<Map<String, Object>> deposit(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Double amount = Double.parseDouble(body.get("amount").toString());
            String description = body.containsKey("description") ? 
                    body.get("description").toString() : null;
            String currency = body.containsKey("currency") && body.get("currency") != null
                    ? body.get("currency").toString()
                    : "USD";
            String idempotencyKey = body.containsKey("idempotencyKey") && body.get("idempotencyKey") != null
                    ? body.get("idempotencyKey").toString()
                    : null;

            WalletDTO wallet;
            try {
                wallet = walletService.deposit(userDetails.getUsername(), amount, description, currency, idempotencyKey);
            } catch (RuntimeException ex) {
                if ("IDEMPOTENT_REPLAY".equals(ex.getMessage())) {
                    wallet = walletService.getWallet(userDetails.getUsername());
                } else {
                    throw ex;
                }
            }
            response.put("success", true);
            response.put("data", wallet);
            response.put("message", "Deposit successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            
            // Null-safe amount extraction
            Object amountObj = body.get("amount");
            if (amountObj == null) {
                throw new IllegalArgumentException("Amount is required");
            }
            Double amount = Double.parseDouble(amountObj.toString());
            
            // Null-safe description extraction
            String description = null;
            if (body.containsKey("description") && body.get("description") != null) {
                description = body.get("description").toString();
            }
            
            // Null-safe withdrawal method extraction
            String withdrawalMethod = null;
            if (body.containsKey("withdrawalMethod") && body.get("withdrawalMethod") != null) {
                withdrawalMethod = body.get("withdrawalMethod").toString();
            }
            
            // Null-safe withdrawal reference extraction
            String withdrawalReference = null;
            if (body.containsKey("withdrawalReference") && body.get("withdrawalReference") != null) {
                withdrawalReference = body.get("withdrawalReference").toString();
            }
            
            // Null-safe account holder name extraction
            String accountHolderName = null;
            if (body.containsKey("accountHolderName") && body.get("accountHolderName") != null) {
                accountHolderName = body.get("accountHolderName").toString();
            }
            
            // Null-safe IFSC code extraction
            String ifscCode = null;
            if (body.containsKey("ifscCode") && body.get("ifscCode") != null) {
                ifscCode = body.get("ifscCode").toString();
            }

            String currency = body.containsKey("currency") && body.get("currency") != null
                    ? body.get("currency").toString()
                    : "USD";
            String idempotencyKey = body.containsKey("idempotencyKey") && body.get("idempotencyKey") != null
                    ? body.get("idempotencyKey").toString()
                    : null;

            WalletDTO wallet;
            try {
                wallet = walletService.withdraw(
                userDetails.getUsername(), 
                amount, 
                description,
                withdrawalMethod,
                withdrawalReference,
                accountHolderName,
                ifscCode,
                currency,
                idempotencyKey
                );
            } catch (RuntimeException ex) {
                if ("IDEMPOTENT_REPLAY".equals(ex.getMessage())) {
                    wallet = walletService.getWallet(userDetails.getUsername());
                } else {
                    throw ex;
                }
            }
            response.put("success", true);
            response.put("data", wallet);
            response.put("message", "Withdrawal successful");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/currency")
    public ResponseEntity<Map<String, Object>> setCurrency(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String currency = body.containsKey("currency") && body.get("currency") != null
                    ? body.get("currency").toString()
                    : "USD";
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setCurrency(currency == null ? "USD" : currency.trim().toUpperCase());
            userRepository.save(user);
            WalletDTO wallet = walletService.getWallet(userDetails.getUsername());
            response.put("success", true);
            response.put("data", wallet);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> transfer(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User fromUser = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Check if using new company-to-researcher transfer format
            if (body.containsKey("fromCompanyId") && body.containsKey("toResearcherId")) {
                String fromCompanyId = body.get("fromCompanyId").toString();
                String toResearcherId = body.get("toResearcherId").toString();
                Double amount = Double.parseDouble(body.get("amount").toString());
                String description = body.containsKey("description") ? 
                        body.get("description").toString() : null;

                // Verify the authenticated user is the company
                if (!fromUser.getId().equals(fromCompanyId)) {
                    throw new RuntimeException("Unauthorized: You can only transfer from your own account");
                }

                Map<String, Object> transferResult = walletService.transferFromCompanyToResearcher(
                    fromCompanyId, toResearcherId, amount, description);
                return ResponseEntity.ok(transferResult);
            } else {
                // Legacy transfer method (email-based)
                String toEmail = body.get("toEmail").toString();
                Double amount = Double.parseDouble(body.get("amount").toString());
                String description = body.containsKey("description") ? 
                        body.get("description").toString() : null;

                WalletDTO wallet = walletService.transfer(userDetails.getUsername(), toEmail, amount, description);
                response.put("success", true);
                response.put("data", wallet);
                response.put("message", "Transfer successful");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<Map<String, Object>> getTransactionHistory(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            List<WalletDTO.TransactionDTO> transactions = walletService.getTransactionHistory(userDetails.getUsername());
            response.put("success", true);
            response.put("data", transactions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}


