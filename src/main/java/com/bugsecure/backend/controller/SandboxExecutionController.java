package com.bugsecure.backend.controller;

import com.bugsecure.backend.model.SandboxExecution;
import com.bugsecure.backend.model.SandboxSession;
import com.bugsecure.backend.repository.SandboxSessionRepository;
import com.bugsecure.backend.repository.UserRepository;
import com.bugsecure.backend.service.SandboxExecutionService;
import com.bugsecure.backend.service.SandboxExecutionService.SandboxExecutionRequest;
import com.bugsecure.backend.service.SandboxExecutionService.SandboxExecutionSecurityScanRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sandbox")
@CrossOrigin(origins = "*")
public class SandboxExecutionController {

    @Autowired
    private SandboxExecutionService executionService;

    @Autowired
    private SandboxSessionRepository sandboxSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/info/{sandboxId}")
    public Map<String, Object> getSandboxInfo(
            @PathVariable String sandboxId,
            Authentication authentication,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            SandboxSession session = sandboxSessionRepository.findById(sandboxId)
                    .orElseThrow(() -> new RuntimeException("Sandbox session not found"));

            // Authorization: allow owner/admin.
            boolean isAdmin = userDetails.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            boolean isOwner = userRepository.findByEmail(userDetails.getUsername())
                    .map(u -> u.getId() != null && u.getId().equals(session.getResearcherUserId()))
                    .orElse(false);

            if (!isAdmin && !isOwner) {
                throw new RuntimeException("Unauthorized to view sandbox info");
            }

            String scheme = request.getScheme();
            String host = request.getServerName();
            Integer port = session.getHostPort();
            String url = (port == null || port <= 0) ? (scheme + "://" + host + "/") : (scheme + "://" + host + ":" + port + "/");

            response.put("success", true);
            response.put("data", Map.of(
                    "sandboxId", session.getId(),
                    "submissionId", session.getSubmissionId(),
                    "status", session.getStatus(),
                    "hostPort", session.getHostPort(),
                    "url", url
            ));
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    @PostMapping("/execute/{sandboxId}")
    public Map<String, Object> execute(
            @PathVariable String sandboxId,
            @RequestBody ExecuteRequest body,
            Authentication authentication
    ) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            if (body == null || body.taskType == null) {
                throw new RuntimeException("taskType is required");
            }

            SandboxExecution execution;
            if ("SECURITY_SCAN".equalsIgnoreCase(body.taskType)) {
                SandboxExecutionSecurityScanRequest req = new SandboxExecutionSecurityScanRequest();
                req.timeoutSeconds = body.timeoutSeconds;
                execution = executionService.executeSecurityScan(sandboxId, userDetails.getUsername(), req);
            } else {
                SandboxExecutionRequest req = new SandboxExecutionRequest();
                req.language = body.language;
                req.sourceCode = body.sourceCode;
                req.sourceFileName = body.sourceFileName;
                req.mainClass = body.mainClass;
                req.timeoutSeconds = body.timeoutSeconds;
                execution = executionService.executeRunCode(sandboxId, userDetails.getUsername(), req);
            }

            response.put("success", true);
            response.put("data", Map.of(
                    "executionId", execution.getId(),
                    "sandboxSessionId", execution.getSandboxSessionId(),
                    "taskType", execution.getTaskType(),
                    "status", execution.getStatus(),
                    "stdout", execution.getStdout(),
                    "stderr", execution.getStderr(),
                    "resultJson", execution.getResultJson(),
                    "failureReason", execution.getFailureReason(),
                    "durationMs", execution.getDurationMs(),
                    "timeoutSeconds", execution.getTimeoutSeconds()
            ));
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    public static class ExecuteRequest {
        public String taskType; // RUN_CODE | SECURITY_SCAN
        public String language; // python|node|java
        public String sourceCode;
        public String sourceFileName;
        public String mainClass; // Java main class
        public Integer timeoutSeconds;
    }
}

