package com.bugsecure.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.LocalDateTime;

@Document(collection = "submission_files")
public class SubmissionFile {

    @Id
    private String id;
    private String fileName;
    private String fileType; // CODE, PDF, DOCUMENT, IMAGE, etc.
    private String mimeType; // application/pdf, image/png, etc.
    private String fileContent; // Base64 encoded content or file path
    private Long fileSize; // File size in bytes
    private LocalDateTime uploadedAt;

    // Secure storage location for file bytes (used for previews).
    private String storageKey;
    private String storageMimeType;
    
    @DBRef
    private CodeSubmission submission;

    // Constructors
    public SubmissionFile() {
    }

    public SubmissionFile(String fileName, String fileType, String mimeType, 
                         String fileContent, Long fileSize, CodeSubmission submission) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.mimeType = mimeType;
        this.fileContent = fileContent;
        this.fileSize = fileSize;
        this.submission = submission;
        this.uploadedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public void setUploadedAtIfNew() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFileContent() {
        return fileContent;
    }

    public void setFileContent(String fileContent) {
        this.fileContent = fileContent;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getStorageMimeType() {
        return storageMimeType;
    }

    public void setStorageMimeType(String storageMimeType) {
        this.storageMimeType = storageMimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public CodeSubmission getSubmission() {
        return submission;
    }

    public void setSubmission(CodeSubmission submission) {
        this.submission = submission;
    }
}







