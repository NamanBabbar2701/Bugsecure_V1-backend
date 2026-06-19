package com.bugsecure.backend.controller;

import com.bugsecure.backend.model.User;
import com.bugsecure.backend.repository.UserRepository;
import com.bugsecure.backend.service.AccountDeletionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AccountDeletionService accountDeletionService;

    @PostMapping("/register")
    public Map<String, Object> registerUser(@RequestBody Map<String, String> body) {
        Map<String, Object> resp = new HashMap<>();
        try {
            if (userRepository.findByEmail(body.get("email")).isPresent()) {
                resp.put("message", "Email already registered");
                resp.put("success", false);
                return resp;
            }
            
            User user = new User();
            user.setUsername(body.get("username"));
            user.setEmail(body.get("email"));
            user.setPassword(passwordEncoder.encode(body.get("password")));
            String requestedRole = body.get("role") != null ? body.get("role") : "USER";
            requestedRole = requestedRole.toUpperCase();
            // Allow ADMIN only for whitelisted emails
            String email = body.get("email");
            boolean isWhitelistedAdmin = email != null && (
                    "goutamp0242@gmail.com".equalsIgnoreCase(email) ||
                    "namanbabbar37@gmail.com".equalsIgnoreCase(email) ||
                    "bugsecure12admin@gmail.com".equalsIgnoreCase(email)
            );
            if ("ADMIN".equals(requestedRole) && !isWhitelistedAdmin) {
                requestedRole = "USER";
            }
            user.setRole(requestedRole);
            user.setCompanyName(body.get("companyName"));
            
            // Handle contract acceptance for hackers/researchers
            if (body.containsKey("contractAccepted") && "USER".equals(requestedRole)) {
                Boolean contractAccepted = Boolean.parseBoolean(body.get("contractAccepted"));
                user.setContractAccepted(contractAccepted);
                if (contractAccepted && body.containsKey("contractHash")) {
                    user.setContractHash(body.get("contractHash"));
                    user.setContractAcceptedAt(java.time.LocalDateTime.now());
                }
            }
            
            userRepository.save(user);
            resp.put("message", "User registered successfully");
            resp.put("success", true);
            return resp;
        } catch (Exception e) {
            e.printStackTrace();
            resp.put("message", "Registration failed: " + e.getMessage());
            resp.put("success", false);
            return resp;
        }
    }

    @DeleteMapping("/me")
    public ResponseEntity<Map<String, Object>> deleteMyAccount(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User me = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if ("ADMIN".equals(me.getRole())) {
                response.put("success", false);
                response.put("error", "Admin account deletion is not allowed via this endpoint");
                return ResponseEntity.badRequest().body(response);
            }

            accountDeletionService.permanentlyDeleteUser(me.getId());
            response.put("success", true);
            response.put("message", "Account deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
