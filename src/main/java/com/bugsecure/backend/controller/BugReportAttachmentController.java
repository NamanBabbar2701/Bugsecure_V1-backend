package com.bugsecure.backend.controller;

import com.bugsecure.backend.model.BugReportAttachment;
import com.bugsecure.backend.model.User;
import com.bugsecure.backend.repository.UserRepository;
import com.bugsecure.backend.service.BugReportAttachmentService;
import com.bugsecure.backend.service.SecureLocalFileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bug-reports")
@CrossOrigin(origins = "*")
public class BugReportAttachmentController {

    @Autowired
    private BugReportAttachmentService attachmentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecureLocalFileStorageService storageService;

    @PostMapping("/{bugReportId}/attachments")
    public ResponseEntity<Map<String, Object>> uploadAttachment(
            @PathVariable String bugReportId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User viewer = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> created = attachmentService.uploadAttachment(bugReportId, file, viewer);
            response.put("success", true);
            response.put("data", created);
            response.put("message", "Attachment uploaded successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{bugReportId}/attachments")
    public ResponseEntity<Map<String, Object>> listAttachments(
            @PathVariable String bugReportId,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User viewer = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Map<String, Object>> attachments = attachmentService.listAttachments(bugReportId, viewer);
            response.put("success", true);
            response.put("data", attachments);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{bugReportId}/attachments/{attachmentId}/preview")
    public ResponseEntity<?> previewAttachment(
            @PathVariable String bugReportId,
            @PathVariable String attachmentId,
            @RequestParam(name = "limit", required = false, defaultValue = "60000") Integer limit,
            Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User viewer = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            BugReportAttachment attachment = attachmentService.getAuthorizedAttachment(bugReportId, attachmentId, viewer);

            String mimeType = attachment.getStorageMimeType();
            if (mimeType == null || mimeType.isBlank()) {
                mimeType = attachment.getMimeType();
            }
            if (mimeType == null || mimeType.isBlank()) {
                mimeType = "application/octet-stream";
            }

            byte[] bytes = storageService.readBytes(attachment.getStorageKey());

            boolean treatAsText = (mimeType.startsWith("text/")) || "CODE".equalsIgnoreCase(attachment.getFileType());
            if (treatAsText) {
                String text = new String(bytes, StandardCharsets.UTF_8);
                int safeLimit = (limit == null || limit < 0) ? 0 : limit;
                if (safeLimit > 0 && text.length() > safeLimit) {
                    text = text.substring(0, safeLimit) + "\n...[truncated]";
                }
                Resource resource = new ByteArrayResource(text.getBytes(StandardCharsets.UTF_8));
                return ResponseEntity.ok()
                        .header("Content-Type", "text/plain; charset=utf-8")
                        .body(resource);
            }

            Resource resource = new ByteArrayResource(bytes);
            return ResponseEntity.ok()
                    .header("Content-Type", mimeType)
                    .body(resource);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

