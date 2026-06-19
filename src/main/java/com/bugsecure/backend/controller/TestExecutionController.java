package com.bugsecure.backend.controller;

import com.bugsecure.backend.model.TestExecution;
import com.bugsecure.backend.model.CodeSubmission;
import com.bugsecure.backend.model.User;
import com.bugsecure.backend.repository.CodeSubmissionRepository;
import com.bugsecure.backend.repository.TestExecutionRepository;
import com.bugsecure.backend.repository.UserRepository;
import com.bugsecure.backend.service.SecureLocalFileStorageService;
import com.bugsecure.backend.service.TestExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/testing")
@CrossOrigin(origins = "*")
public class TestExecutionController {

    @Autowired
    private TestExecutionService testExecutionService;

    @Autowired
    private TestExecutionRepository testExecutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecureLocalFileStorageService storageService;

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runTest(
            @RequestBody Map<String, Object> testRequest,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Map<String, Object> result = testExecutionService.runTest(userDetails.getUsername(), testRequest);
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/result/{executionId}")
    public ResponseEntity<Map<String, Object>> getTestResult(
            @PathVariable String executionId,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Map<String, Object> result = testExecutionService.getTestResult(userDetails.getUsername(), executionId);
            response.put("success", true);
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getTestHistory(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            List<TestExecution> history = testExecutionService.getTestHistory(userDetails.getUsername());
            response.put("success", true);
            response.put("data", history);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Secure preview of uploaded script/file used in a test execution.
     * Authorizations:
     * - ADMIN: always
     * - USER: only the owning researcher
     * - COMPANY: only the owning submission company (if submission linked)
     */
    @GetMapping("/executions/{executionId}/preview")
    public ResponseEntity<?> previewExecutionScript(
            @PathVariable String executionId,
            @RequestParam(name = "limit", required = false, defaultValue = "60000") Integer limit,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User viewer = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            TestExecution execution = testExecutionRepository.findById(executionId)
                    .orElseThrow(() -> new RuntimeException("Test execution not found"));

            boolean authorized = false;
            if ("ADMIN".equals(viewer.getRole())) {
                authorized = true;
            } else if ("USER".equals(viewer.getRole())) {
                authorized = execution.getResearcher() != null && execution.getResearcher().getId().equals(viewer.getId());
            } else if ("COMPANY".equals(viewer.getRole())) {
                // Only allow preview if execution is linked to a submission and viewer matches submission company.
                if (execution.getSubmission() != null && execution.getSubmission().getCompany() != null) {
                    authorized = execution.getSubmission().getCompany().getId().equals(viewer.getId());
                }
            }

            if (!authorized) {
                return ResponseEntity.status(403).body(Map.of("success", false, "error", "Unauthorized"));
            }

            byte[] bytes;
            if (execution.getScriptStorageKey() != null && !execution.getScriptStorageKey().isBlank()) {
                bytes = storageService.readBytes(execution.getScriptStorageKey());
            } else if (execution.getScriptContent() != null) {
                bytes = execution.getScriptContent().getBytes(StandardCharsets.UTF_8);
            } else {
                bytes = new byte[0];
            }

            String text = new String(bytes, StandardCharsets.UTF_8);
            int safeLimit = limit == null || limit < 0 ? 0 : limit;
            if (safeLimit > 0 && text.length() > safeLimit) {
                text = text.substring(0, safeLimit) + "\n...[truncated]";
            }

            Resource resource = new ByteArrayResource(text.getBytes(StandardCharsets.UTF_8));
            return ResponseEntity.ok()
                    .header("Content-Type", "text/plain; charset=utf-8")
                    .body(resource);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}






