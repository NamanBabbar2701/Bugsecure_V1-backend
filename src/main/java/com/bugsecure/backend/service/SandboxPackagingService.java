package com.bugsecure.backend.service;

import com.bugsecure.backend.model.CodeSubmission;
import com.bugsecure.backend.model.SubmissionFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class SandboxPackagingService {

    @Autowired
    private SecureLocalFileStorageService storageService;

    @Value("${sandbox.work-dir:./sandbox_workdir}")
    private String workDir;

    // Simple safety guard to prevent huge sandboxes from filling disks.
    @Value("${sandbox.max-total-bytes:52428800}") // 50MB
    private long maxTotalBytes;

    public static class PackagedSubmission {
        private final Path localDir;

        public PackagedSubmission(Path localDir) {
            this.localDir = localDir;
        }

        public Path getLocalDir() {
            return localDir;
        }
    }

    public PackagedSubmission packageSubmission(CodeSubmission submission, List<SubmissionFile> files) {
        try {
            Path root = Paths.get(workDir).toAbsolutePath().normalize();
            Path sessionDir = root.resolve("session_" + UUID.randomUUID().toString().replace("-", "")).normalize();
            Path wwwDir = sessionDir.resolve("www");
            Files.createDirectories(wwwDir);

            long totalBytes = 0;

            // Primary code file (legacy + new uploads)
            String primaryName = submission.getFileName();
            if (primaryName == null || primaryName.isBlank()) {
                primaryName = guessPrimaryFileName(submission);
            }

            byte[] primaryBytes = resolveSubmissionPrimaryBytes(submission);
            totalBytes += primaryBytes.length;
            enforceLimit(totalBytes, "primary code");
            writeFileSafe(wwwDir, primaryName, primaryBytes);

            // Additional submission files
            for (SubmissionFile f : files) {
                if (f.getFileName() == null || f.getFileName().isBlank()) continue;

                byte[] bytes = resolveFileBytes(f);
                totalBytes += bytes.length;
                enforceLimit(totalBytes, "submission file: " + f.getFileName());

                // For now we flatten to a single directory for simplicity and safety.
                writeFileSafe(wwwDir, f.getFileName(), bytes);
            }

            return new PackagedSubmission(sessionDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to package submission: " + e.getMessage(), e);
        }
    }

    public void deleteQuietly(Path dir) {
        if (dir == null) return;
        try {
            if (Files.exists(dir)) {
                // Recursively delete. (Directory may contain multiple files.)
                Files.walk(dir)
                        .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {
                            }
                        });
            }
        } catch (IOException ignored) {
        }
    }

    private void enforceLimit(long bytesSoFar, String context) {
        if (bytesSoFar > maxTotalBytes) {
            throw new RuntimeException("Submission too large for sandbox (" + context + ")");
        }
    }

    private byte[] resolveSubmissionPrimaryBytes(CodeSubmission submission) {
        if (submission.getCodeStorageKey() != null && !submission.getCodeStorageKey().isBlank()) {
            return storageService.readBytes(submission.getCodeStorageKey());
        }
        // Legacy fallback
        if (submission.getCodeContent() != null) {
            return submission.getCodeContent().getBytes(StandardCharsets.UTF_8);
        }
        return new byte[0];
    }

    private byte[] resolveFileBytes(SubmissionFile f) {
        if (f.getStorageKey() != null && !f.getStorageKey().isBlank()) {
            return storageService.readBytes(f.getStorageKey());
        }
        // Fallback if older records store embedded content
        if (f.getFileContent() != null && !f.getFileContent().isBlank()) {
            return SecureLocalFileStorageService.decodeContentToBytes(f.getFileContent());
        }
        return new byte[0];
    }

    private String guessPrimaryFileName(CodeSubmission submission) {
        String mime = submission.getCodeMimeType() != null ? submission.getCodeMimeType() : "";
        String m = mime.toLowerCase(Locale.ROOT);
        if (m.contains("html")) return "index.html";
        if (m.contains("javascript") || m.contains("js")) return "script.js";
        return "code.txt";
    }

    private void writeFileSafe(Path wwwDir, String originalFileName, byte[] bytes) throws IOException {
        String safe = sanitizeFileName(originalFileName);
        Path target = wwwDir.resolve(safe).normalize();
        if (!target.startsWith(wwwDir)) {
            throw new RuntimeException("Invalid filename for sandbox: " + originalFileName);
        }
        Files.write(target, bytes);
    }

    private String sanitizeFileName(String original) {
        String name = original.replace("\\", "/");
        // Disallow directory traversal: keep only the last segment.
        if (name.contains("/")) name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replace(":", "_");
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        return name == null || name.isBlank() ? "file.txt" : name;
    }
}

