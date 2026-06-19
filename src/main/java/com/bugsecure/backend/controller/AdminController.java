package com.bugsecure.backend.controller;

import com.bugsecure.backend.dto.BugReportDTO;
import com.bugsecure.backend.dto.CodeSubmissionDTO;
import com.bugsecure.backend.dto.UserDTO;
import com.bugsecure.backend.model.User;
import com.bugsecure.backend.model.Payment;
import com.bugsecure.backend.repository.UserRepository;
import com.bugsecure.backend.repository.PaymentRepository;
import com.bugsecure.backend.service.BugReportService;
import com.bugsecure.backend.service.CodeSubmissionService;
import com.bugsecure.backend.service.AccountDeletionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CodeSubmissionService codeSubmissionService;

    @Autowired
    private BugReportService bugReportService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AccountDeletionService accountDeletionService;

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User admin = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isWhitelisted = admin.getEmail() != null && (
                    "goutamp0242@gmail.com".equalsIgnoreCase(admin.getEmail()) ||
                    "namanbabbar37@gmail.com".equalsIgnoreCase(admin.getEmail()) ||
                    "bugsecure12admin@gmail.com".equalsIgnoreCase(admin.getEmail())
            );
            if (!"ADMIN".equals(admin.getRole()) || !isWhitelisted) {
                response.put("success", false);
                response.put("error", "Unauthorized: Admin access required");
                return ResponseEntity.status(403).body(response);
            }

            List<UserDTO> users = userRepository.findAll().stream().map(user -> {
                UserDTO dto = new UserDTO();
                dto.setId(user.getId());
                dto.setUsername(user.getUsername());
                dto.setEmail(user.getEmail());
                dto.setRole(user.getRole());
                dto.setCompanyName(user.getCompanyName());
                return dto;
            }).collect(Collectors.toList());

            response.put("success", true);
            response.put("data", users);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/submissions")
    public ResponseEntity<Map<String, Object>> getAllSubmissions(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User admin = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isWhitelisted = admin.getEmail() != null && (
                    "goutamp0242@gmail.com".equalsIgnoreCase(admin.getEmail()) ||
                    "namanbabbar37@gmail.com".equalsIgnoreCase(admin.getEmail()) ||
                    "bugsecure12admin@gmail.com".equalsIgnoreCase(admin.getEmail())
            );
            if (!"ADMIN".equals(admin.getRole()) || !isWhitelisted) {
                response.put("success", false);
                response.put("error", "Unauthorized: Admin access required");
                return ResponseEntity.status(403).body(response);
            }

            List<CodeSubmissionDTO> submissions = codeSubmissionService.getAllSubmissions();
            response.put("success", true);
            response.put("data", submissions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/bug-reports")
    public ResponseEntity<Map<String, Object>> getAllBugReports(
            Authentication authentication,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "severity", required = false) String severity,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10000") Integer pageSize
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User admin = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isWhitelisted = admin.getEmail() != null && (
                    "goutamp0242@gmail.com".equalsIgnoreCase(admin.getEmail()) ||
                    "namanbabbar37@gmail.com".equalsIgnoreCase(admin.getEmail()) ||
                    "bugsecure12admin@gmail.com".equalsIgnoreCase(admin.getEmail())
            );
            if (!"ADMIN".equals(admin.getRole()) || !isWhitelisted) {
                response.put("success", false);
                response.put("error", "Unauthorized: Admin access required");
                return ResponseEntity.status(403).body(response);
            }

            BugReportService.PaginatedResult<BugReportDTO> result = bugReportService.searchBugReportsForAdmin(
                    q,
                    status,
                    severity,
                    page == null ? 0 : page,
                    pageSize == null ? 10000 : pageSize
            );

            response.put("success", true);
            response.put("data", result.getItems());
            Map<String, Object> meta = new HashMap<>();
            meta.put("total", result.getTotal());
            meta.put("page", result.getPage());
            meta.put("pageSize", result.getPageSize());
            meta.put("totalPages", result.getTotalPages());
            response.put("meta", meta);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User admin = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isWhitelisted = admin.getEmail() != null && (
                    "goutamp0242@gmail.com".equalsIgnoreCase(admin.getEmail()) ||
                    "namanbabbar37@gmail.com".equalsIgnoreCase(admin.getEmail()) ||
                    "bugsecure12admin@gmail.com".equalsIgnoreCase(admin.getEmail())
            );
            if (!"ADMIN".equals(admin.getRole()) || !isWhitelisted) {
                response.put("success", false);
                response.put("error", "Unauthorized: Admin access required");
                return ResponseEntity.status(403).body(response);
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", userRepository.count());
            stats.put("totalSubmissions", codeSubmissionService.getAllSubmissions().size());
            stats.put("totalBugReports", bugReportService.getAllBugReports().size());
            stats.put("totalCompanies", userRepository.findAll().stream()
                    .filter(u -> "COMPANY".equals(u.getRole())).count());
            stats.put("totalResearchers", userRepository.findAll().stream()
                    .filter(u -> "USER".equals(u.getRole())).count());

            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/payments/{paymentId}/commission-details")
    public ResponseEntity<Map<String, Object>> getPaymentCommissionDetails(
            @PathVariable String paymentId,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User admin = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isWhitelisted = admin.getEmail() != null && (
                    "goutamp0242@gmail.com".equalsIgnoreCase(admin.getEmail()) ||
                    "namanbabbar37@gmail.com".equalsIgnoreCase(admin.getEmail()) ||
                    "bugsecure12admin@gmail.com".equalsIgnoreCase(admin.getEmail())
            );
            if (!"ADMIN".equals(admin.getRole()) || !isWhitelisted) {
                response.put("success", false);
                response.put("error", "Unauthorized: Admin access required");
                return ResponseEntity.status(403).body(response);
            }

            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new RuntimeException("Payment not found"));

            Double bountyUSD = payment.getBountyAmountUSD() != null ? payment.getBountyAmountUSD() : payment.getAmountUSD();
            Double netUSD = payment.getResearcherNetAmountUSD() != null ? payment.getResearcherNetAmountUSD() : payment.getAmountUSD();
            Double commissionUSD = payment.getPlatformCommissionAmountUSD() != null ? payment.getPlatformCommissionAmountUSD()
                    : (bountyUSD != null ? (bountyUSD * 0.05) : 0.0);

            Map<String, Object> data = new HashMap<>();
            data.put("bountyAmountUSD", bountyUSD);
            data.put("researcherNetAmountUSD", netUSD);
            data.put("platformCommissionAmountUSD", commissionUSD);
            data.put("bountyAmountINR", payment.getBountyAmountINR());
            data.put("researcherNetAmountINR", payment.getResearcherNetAmountINR());
            data.put("platformCommissionAmountINR", payment.getPlatformCommissionAmountINR());

            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUserAsAdmin(
            @PathVariable String userId,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User admin = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isWhitelisted = admin.getEmail() != null && (
                    "goutamp0242@gmail.com".equalsIgnoreCase(admin.getEmail()) ||
                    "namanbabbar37@gmail.com".equalsIgnoreCase(admin.getEmail()) ||
                    "bugsecure12admin@gmail.com".equalsIgnoreCase(admin.getEmail())
            );

            if (!"ADMIN".equals(admin.getRole()) || !isWhitelisted) {
                response.put("success", false);
                response.put("error", "Unauthorized: Admin access required");
                return ResponseEntity.status(403).body(response);
            }

            if (userId == null || userId.isBlank()) {
                response.put("success", false);
                response.put("error", "Invalid user id");
                return ResponseEntity.badRequest().body(response);
            }

            accountDeletionService.permanentlyDeleteUser(userId);
            response.put("success", true);
            response.put("message", "User deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}






