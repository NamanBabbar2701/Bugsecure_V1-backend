package com.bugsecure.backend.controller;

import com.bugsecure.backend.model.SandboxSession;
import com.bugsecure.backend.service.SandboxService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sandbox")
@CrossOrigin(origins = "*")
public class SandboxController {

    @Autowired
    private SandboxService sandboxService;

    @PostMapping("/start/{submissionId}")
    public Map<String, Object> startSandbox(
            @PathVariable String submissionId,
            Authentication authentication,
            HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String email = userDetails.getUsername();

            SandboxSession session = sandboxService.startSandbox(submissionId, email);
            String url = buildSandboxUrl(request, session);

            response.put("success", true);
            response.put("data", Map.of(
                    "sandboxId", session.getId(),
                    "submissionId", session.getSubmissionId(),
                    "hostPort", session.getHostPort(),
                    "url", url,
                    "status", session.getStatus()
            ));
            response.put("message", "Sandbox started successfully");
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    @DeleteMapping("/stop/{sandboxId}")
    public Map<String, Object> stopSandbox(
            @PathVariable String sandboxId,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String email = userDetails.getUsername();

            SandboxSession session = sandboxService.stopSandbox(sandboxId, email);
            response.put("success", true);
            response.put("data", Map.of(
                    "sandboxId", session.getId(),
                    "status", session.getStatus()
            ));
            response.put("message", "Sandbox stopped successfully");
            return response;
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    private String buildSandboxUrl(HttpServletRequest request, SandboxSession session) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        Integer port = session.getHostPort();
        if (port == null || port <= 0) {
            return scheme + "://" + host + "/";
        }
        return scheme + "://" + host + ":" + port + "/";
    }
}

