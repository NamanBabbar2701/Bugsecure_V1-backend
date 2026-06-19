package com.bugsecure.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.LocalDateTime;

@Document(collection = "test_executions")
public class TestExecution {

    @Id
    private String id;
    
    @DBRef
    private User researcher; // The researcher running the test
    
    @DBRef
    private CodeSubmission submission; // The program being tested (optional)
    
    private String testType; // SCRIPT, COMMAND, FILE_UPLOAD
    private String scriptContent; // The script or command content
    private String fileName; // If file was uploaded
    private String fileType; // JS, PYTHON, etc.
    // Secure storage for uploaded script/file bytes. New executions should prefer this.
    private String scriptStorageKey;
    private String scriptMimeType;
    private Long scriptSizeBytes;
    private String status = "PENDING"; // PENDING, RUNNING, COMPLETED, FAILED
    private String output; // Test execution output
    private String errorLog; // Error logs if any
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Long executionTimeMs; // Execution time in milliseconds

    // Constructors
    public TestExecution() {
    }

    public TestExecution(User researcher, CodeSubmission submission, String testType, String scriptContent) {
        this.researcher = researcher;
        this.submission = submission;
        this.testType = testType;
        this.scriptContent = scriptContent;
        this.createdAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getResearcher() {
        return researcher;
    }

    public void setResearcher(User researcher) {
        this.researcher = researcher;
    }

    public CodeSubmission getSubmission() {
        return submission;
    }

    public void setSubmission(CodeSubmission submission) {
        this.submission = submission;
    }

    public String getTestType() {
        return testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public String getScriptContent() {
        return scriptContent;
    }

    public void setScriptContent(String scriptContent) {
        this.scriptContent = scriptContent;
    }

    public String getScriptStorageKey() {
        return scriptStorageKey;
    }

    public void setScriptStorageKey(String scriptStorageKey) {
        this.scriptStorageKey = scriptStorageKey;
    }

    public String getScriptMimeType() {
        return scriptMimeType;
    }

    public void setScriptMimeType(String scriptMimeType) {
        this.scriptMimeType = scriptMimeType;
    }

    public Long getScriptSizeBytes() {
        return scriptSizeBytes;
    }

    public void setScriptSizeBytes(Long scriptSizeBytes) {
        this.scriptSizeBytes = scriptSizeBytes;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getErrorLog() {
        return errorLog;
    }

    public void setErrorLog(String errorLog) {
        this.errorLog = errorLog;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
    
    public void setCreatedAtIfNew() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
    
    public void markCompleted() {
        this.completedAt = LocalDateTime.now();
        this.status = "COMPLETED";
    }
    
    public void markFailed() {
        this.completedAt = LocalDateTime.now();
        this.status = "FAILED";
    }
}






