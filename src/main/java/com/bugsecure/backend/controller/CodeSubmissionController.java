package com.bugsecure.backend.controller;

import com.bugsecure.backend.dto.CodeSubmissionDTO;
import com.bugsecure.backend.model.SubmissionFile;
import com.bugsecure.backend.service.CodeSubmissionService;
import com.bugsecure.backend.service.SecureLocalFileStorageService;
import com.bugsecure.backend.repository.CodeSubmissionRepository;
import com.bugsecure.backend.repository.SubmissionFileRepository;
import com.bugsecure.backend.repository.UserRepository;
import com.bugsecure.backend.model.CodeSubmission;
import com.bugsecure.backend.model.User;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/submissions")
@CrossOrigin(origins = "*")
public class CodeSubmissionController {

    @Autowired
    private CodeSubmissionService codeSubmissionService;

    @Autowired
    private CodeSubmissionRepository codeSubmissionRepository;

    @Autowired
    private SubmissionFileRepository submissionFileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecureLocalFileStorageService storageService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSubmission(
            @RequestBody CodeSubmissionDTO dto,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            CodeSubmissionDTO created = codeSubmissionService.createSubmission(dto, userDetails.getUsername());
            response.put("success", true);
            response.put("data", created);
            response.put("message", "Submission created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllSubmissions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<CodeSubmissionDTO> submissions;
            
            if (status != null && status.equals("open")) {
                if (sort != null) {
                    submissions = codeSubmissionService.getOpenSubmissionsSorted(sort);
                } else {
                    submissions = codeSubmissionService.getOpenSubmissions();
                }
            } else {
                if (sort != null) {
                    submissions = codeSubmissionService.getAllSubmissionsSorted(sort);
                } else {
                    submissions = codeSubmissionService.getAllSubmissions();
                }
            }
            
            response.put("success", true);
            response.put("data", submissions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/my-submissions")
    public ResponseEntity<Map<String, Object>> getMySubmissions(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            List<CodeSubmissionDTO> submissions = codeSubmissionService.getSubmissionsByCompany(userDetails.getUsername());
            response.put("success", true);
            response.put("data", submissions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getSubmissionById(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            CodeSubmissionDTO submission = codeSubmissionService.getSubmissionById(id);
            response.put("success", true);
            response.put("data", submission);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Secure code preview for authorized users.
     * - COMPANY: owning company
     * - USER: only if contractAccepted
     * - ADMIN: all
     *
     * Returns text/plain with optional truncation to reduce payload size.
     */
    @GetMapping("/{id}/code/preview")
    public ResponseEntity<?> previewCode(
            @PathVariable("id") String submissionId,
            @RequestParam(name = "limit", required = false, defaultValue = "60000") Integer limit,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User viewer = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            CodeSubmission submission = codeSubmissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("Submission not found"));

            boolean authorized = false;
            if ("ADMIN".equals(viewer.getRole())) {
                authorized = true;
            } else if ("COMPANY".equals(viewer.getRole())) {
                authorized = submission.getCompany() != null && viewer.getId().equals(submission.getCompany().getId());
            } else if ("USER".equals(viewer.getRole())) {
                authorized = viewer.getContractAccepted() != null && viewer.getContractAccepted();
            }

            if (!authorized) {
                return ResponseEntity.status(403).body(Map.of("success", false, "error", "Unauthorized"));
            }

            byte[] bytes;
            String mimeType = "text/plain";
            if (submission.getCodeStorageKey() != null && !submission.getCodeStorageKey().isBlank()) {
                bytes = storageService.readBytes(submission.getCodeStorageKey());
                if (submission.getCodeMimeType() != null && !submission.getCodeMimeType().isBlank()) {
                    mimeType = submission.getCodeMimeType();
                }
            } else if (submission.getCodeContent() != null) {
                bytes = submission.getCodeContent().getBytes(StandardCharsets.UTF_8);
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
                    .header("Content-Type", mimeType)
                    .body(resource);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Secure preview for an uploaded submission file.
     * Returns bytes for binary previews or text/plain for text/code previews (truncated by limit).
     */
    @GetMapping("/files/{fileId}/preview")
    public ResponseEntity<?> previewSubmissionFile(
            @PathVariable("fileId") String fileId,
            @RequestParam(name = "limit", required = false, defaultValue = "60000") Integer limit,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User viewer = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            SubmissionFile file = submissionFileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            CodeSubmission submission = file.getSubmission();

            boolean authorized = false;
            if ("ADMIN".equals(viewer.getRole())) {
                authorized = true;
            } else if ("COMPANY".equals(viewer.getRole())) {
                authorized = submission != null && submission.getCompany() != null && viewer.getId().equals(submission.getCompany().getId());
            } else if ("USER".equals(viewer.getRole())) {
                authorized = viewer.getContractAccepted() != null && viewer.getContractAccepted();
            }

            if (!authorized) {
                return ResponseEntity.status(403).body(Map.of("success", false, "error", "Unauthorized"));
            }

            byte[] bytes;
            String mimeType = "application/octet-stream";
            if (file.getStorageKey() != null && !file.getStorageKey().isBlank()) {
                bytes = storageService.readBytes(file.getStorageKey());
                if (file.getStorageMimeType() != null && !file.getStorageMimeType().isBlank()) {
                    mimeType = file.getStorageMimeType();
                } else if (file.getMimeType() != null && !file.getMimeType().isBlank()) {
                    mimeType = file.getMimeType();
                }
            } else if (file.getFileContent() != null) {
                bytes = SecureLocalFileStorageService.decodeContentToBytes(file.getFileContent());
                if (file.getMimeType() != null && !file.getMimeType().isBlank()) {
                    mimeType = file.getMimeType();
                }
            } else {
                bytes = new byte[0];
            }

            boolean treatAsText = mimeType != null && mimeType.startsWith("text/");
            // Also treat common code mime as text.
            if (!treatAsText && file.getFileType() != null) {
                String t = file.getFileType().toUpperCase();
                treatAsText = "CODE".equals(t);
            }

            if (treatAsText) {
                String text = new String(bytes, StandardCharsets.UTF_8);
                int safeLimit = limit == null || limit < 0 ? 0 : limit;
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
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateSubmission(
            @PathVariable String id,
            @RequestBody CodeSubmissionDTO dto,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            CodeSubmissionDTO updated = codeSubmissionService.updateSubmission(id, dto, userDetails.getUsername());
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "Submission updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateSubmissionStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String status = body.get("status");
            CodeSubmissionDTO updated = codeSubmissionService.updateSubmissionStatusOnly(id, status, userDetails.getUsername());
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "Submission status updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteSubmission(
            @PathVariable String id,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            codeSubmissionService.deleteSubmission(id, userDetails.getUsername());
            response.put("success", true);
            response.put("message", "Submission deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

