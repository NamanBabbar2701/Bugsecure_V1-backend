package com.bugsecure.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Local file storage for secure uploads.
 *
 * Important:
 * - storageKey is a relative path under file.storage.root
 * - preview endpoints must enforce authorization before returning any bytes
 */
@Service
public class SecureLocalFileStorageService {

    @Value("${file.storage.root:./secure_uploads}")
    private String fileStorageRoot;

    // Legacy root used by older records / earlier deployments.
    @Value("${file.storage.legacy-root:./uploads}")
    private String fileStorageLegacyRoot;

    public static class StoredFile {
        private final String storageKey;
        private final String mimeType;
        private final Long sizeBytes;

        public StoredFile(String storageKey, String mimeType, Long sizeBytes) {
            this.storageKey = storageKey;
            this.mimeType = mimeType;
            this.sizeBytes = sizeBytes;
        }

        public String getStorageKey() {
            return storageKey;
        }

        public String getMimeType() {
            return mimeType;
        }

        public Long getSizeBytes() {
            return sizeBytes;
        }
    }

    public StoredFile storeBytes(String folder, String originalFileName, String mimeType, byte[] bytes) {
        try {
            if (bytes == null) {
                bytes = new byte[0];
            }

            String safeName = sanitizeFileName(originalFileName);
            String ext = "";
            int dotIdx = safeName.lastIndexOf('.');
            if (dotIdx > 0 && dotIdx < safeName.length() - 1) {
                ext = safeName.substring(dotIdx);
            }

            String uuid = UUID.randomUUID().toString().replace("-", "");
            String finalName = uuid + ext;

            Path root = Paths.get(fileStorageRoot).toAbsolutePath().normalize();
            Path dir = root.resolve(folder).normalize();
            Files.createDirectories(dir);

            Path target = dir.resolve(finalName).normalize();

            // Safety: ensure target remains under root
            if (!target.startsWith(root)) {
                throw new RuntimeException("Invalid storage path");
            }

            Files.write(target, bytes);

            String storageKey = Paths.get(folder).resolve(finalName).toString().replace("\\", "/");
            return new StoredFile(storageKey, mimeType, bytes.length == 0 ? 0L : (long) bytes.length);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage());
        }
    }

    public byte[] readBytes(String storageKey) {
        try {
            if (storageKey == null || storageKey.isBlank()) {
                return new byte[0];
            }

            Path root = Paths.get(fileStorageRoot).toAbsolutePath().normalize();
            Path legacyRoot = Paths.get(fileStorageLegacyRoot).toAbsolutePath().normalize();

            Path storagePath = Paths.get(storageKey);

            // Some historical records may contain absolute paths.
            // For safety, only allow reading from our configured roots.
            if (storagePath.isAbsolute()) {
                Path abs = storagePath.toAbsolutePath().normalize();
                if (abs.startsWith(root) || abs.startsWith(legacyRoot)) {
                    return Files.readAllBytes(abs);
                }
                throw new RuntimeException("Invalid storage key (outside allowed roots)");
            }

            Path target = root.resolve(storageKey).normalize();
            if (!target.startsWith(root)) {
                // Fallback to legacy root for older persisted keys.
                Path legacyTarget = legacyRoot.resolve(storageKey).normalize();
                if (legacyTarget.startsWith(legacyRoot)) {
                    return Files.readAllBytes(legacyTarget);
                }
                throw new RuntimeException("Invalid storage key");
            }

            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + e.getMessage());
        }
    }

    public String bytesToBase64Sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(bytes));
        } catch (Exception e) {
            return Instant.now().toString();
        }
    }

    public static boolean looksLikeDataUrl(String content) {
        return content != null && content.startsWith("data:") && content.contains(";base64,");
    }

    public static byte[] decodeContentToBytes(String content) {
        if (content == null) return new byte[0];
        if (!looksLikeDataUrl(content)) {
            // Treat as plain text
            return content.getBytes(StandardCharsets.UTF_8);
        }
        // data:<mime>;base64,<payload>
        int commaIdx = content.indexOf(',');
        String base64Part = commaIdx >= 0 ? content.substring(commaIdx + 1) : "";
        return Base64.getDecoder().decode(base64Part);
    }

    public static String extractMimeTypeFromDataUrl(String content) {
        if (!looksLikeDataUrl(content)) return null;
        // data:<mime>;base64,<payload>
        int start = "data:".length();
        int end = content.indexOf(';', start);
        if (end > start) {
            return content.substring(start, end);
        }
        return null;
    }

    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) return "file";
        // Remove path separators and allow only safe characters.
        String justName = originalFileName.replace("\\", "/");
        if (justName.contains("/")) {
            justName = justName.substring(justName.lastIndexOf('/') + 1);
        }
        return justName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

