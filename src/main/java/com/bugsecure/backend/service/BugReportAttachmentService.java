package com.bugsecure.backend.service;

import com.bugsecure.backend.model.BugReport;
import com.bugsecure.backend.model.BugReportAttachment;
import com.bugsecure.backend.model.CodeSubmission;
import com.bugsecure.backend.model.User;
import com.bugsecure.backend.repository.BugReportAttachmentRepository;
import com.bugsecure.backend.repository.BugReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BugReportAttachmentService {

    @Autowired
    private BugReportRepository bugReportRepository;

    @Autowired
    private BugReportAttachmentRepository attachmentRepository;

    @Autowired
    private SecureLocalFileStorageService storageService;

    private static final Set<String> CODE_EXTS = Set.of(
            "js", "ts", "tsx", "jsx",
            "py", "txt", "sh",
            "c", "cc", "cpp",
            "cs",
            "go",
            "java",
            "rb",
            "php",
            "html",
            "css"
    );

    public Map<String, Object> uploadAttachment(String bugReportId, MultipartFile file, User viewer) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        BugReport bugReport = bugReportRepository.findById(bugReportId)
                .orElseThrow(() -> new RuntimeException("Bug report not found"));

        // Upload: only original reporter.
        if (!isReporter(bugReport, viewer)) {
            throw new RuntimeException("Unauthorized to upload attachments for this bug report");
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment";
        String mimeType = file.getContentType();
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }

        String inferredFileType = inferFileType(originalName, mimeType);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read uploaded file: " + e.getMessage());
        }

        String folder = "bug_report_attachments/" + bugReportId + "/" + viewer.getId() + "/" + java.util.UUID.randomUUID().toString().replace("-", "");
        SecureLocalFileStorageService.StoredFile stored = storageService.storeBytes(folder, originalName, mimeType, bytes);

        BugReportAttachment attachment = new BugReportAttachment();
        attachment.setBugReport(bugReport);
        attachment.setUploader(viewer);
        attachment.setOriginalFileName(originalName);
        attachment.setFileType(inferredFileType);
        attachment.setMimeType(mimeType);
        attachment.setSizeBytes(stored.getSizeBytes());
        attachment.setStorageKey(stored.getStorageKey());
        attachment.setStorageMimeType(stored.getMimeType());
        attachment.setUploadedAt(LocalDateTime.now());

        BugReportAttachment saved = attachmentRepository.save(attachment);

        return toAttachmentMetadata(saved);
    }

    public List<Map<String, Object>> listAttachments(String bugReportId, User viewer) {
        BugReport bugReport = bugReportRepository.findById(bugReportId)
                .orElseThrow(() -> new RuntimeException("Bug report not found"));

        // List/preview rules:
        // ADMIN: allowed
        // COMPANY: owning company
        // USER: only original reporter
        if (!isViewerAuthorized(bugReport, viewer)) {
            throw new RuntimeException("Unauthorized to view attachments for this bug report");
        }

        List<BugReportAttachment> attachments = attachmentRepository.findByBugReport(bugReport);
        return attachments.stream()
                .map(this::toAttachmentMetadata)
                .collect(Collectors.toList());
    }

    public BugReportAttachment getAuthorizedAttachment(String bugReportId, String attachmentId, User viewer) {
        BugReport bugReport = bugReportRepository.findById(bugReportId)
                .orElseThrow(() -> new RuntimeException("Bug report not found"));

        if (!isViewerAuthorized(bugReport, viewer)) {
            throw new RuntimeException("Unauthorized to preview attachments for this bug report");
        }

        return attachmentRepository.findByIdAndBugReport(attachmentId, bugReport)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));
    }

    private boolean isViewerAuthorized(BugReport bugReport, User viewer) {
        if (viewer == null) return false;
        String role = viewer.getRole();
        if ("ADMIN".equals(role)) return true;
        if ("COMPANY".equals(role)) return isOwningCompany(bugReport, viewer);
        if ("USER".equals(role)) return isReporter(bugReport, viewer);
        return false;
    }

    private boolean isReporter(BugReport bugReport, User viewer) {
        if (bugReport == null || bugReport.getReporter() == null || viewer == null) return false;
        return bugReport.getReporter().getId() != null && bugReport.getReporter().getId().equals(viewer.getId());
    }

    private boolean isOwningCompany(BugReport bugReport, User viewer) {
        if (bugReport == null || viewer == null) return false;
        CodeSubmission submission = bugReport.getCodeSubmission();
        if (submission == null || submission.getCompany() == null) return false;
        return submission.getCompany().getId() != null && submission.getCompany().getId().equals(viewer.getId());
    }

    private String inferFileType(String originalFileName, String mimeType) {
        String ext = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            ext = originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase();
        }

        String mt = mimeType != null ? mimeType.toLowerCase() : "";
        if (mt.startsWith("text/") || CODE_EXTS.contains(ext)) {
            return "CODE";
        }
        if (mt.startsWith("image/")) {
            return "IMAGE";
        }
        return "DOCUMENT";
    }

    private Map<String, Object> toAttachmentMetadata(BugReportAttachment attachment) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("id", attachment.getId());
        meta.put("name", attachment.getOriginalFileName());
        meta.put("type", attachment.getFileType());
        meta.put("mimeType", attachment.getMimeType() != null ? attachment.getMimeType() : attachment.getStorageMimeType());
        meta.put("size", attachment.getSizeBytes() != null ? attachment.getSizeBytes() : 0L);
        meta.put("uploadedAt", attachment.getUploadedAt());
        return meta;
    }
}

