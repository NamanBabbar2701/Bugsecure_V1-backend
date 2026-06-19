package com.bugsecure.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "sandbox_executions")
public class SandboxExecution {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    private String id;

    private String sandboxSessionId;
    private String submissionId;
    private String researcherUserId;

    // RUN_CODE or SECURITY_SCAN
    private String taskType;

    // RUN_CODE
    private String language;
    private String mainClass;
    private String sourceFileName;

    // Outputs
    private String stdout;
    private String stderr;

    // Best-effort structured output (stored as string to keep model simple)
    private String resultJson;

    private String status = STATUS_RUNNING;
    private String failureReason;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Long durationMs;
    private Integer timeoutSeconds;

    public SandboxExecution() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSandboxSessionId() { return sandboxSessionId; }
    public void setSandboxSessionId(String sandboxSessionId) { this.sandboxSessionId = sandboxSessionId; }

    public String getSubmissionId() { return submissionId; }
    public void setSubmissionId(String submissionId) { this.submissionId = submissionId; }

    public String getResearcherUserId() { return researcherUserId; }
    public void setResearcherUserId(String researcherUserId) { this.researcherUserId = researcherUserId; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getMainClass() { return mainClass; }
    public void setMainClass(String mainClass) { this.mainClass = mainClass; }

    public String getSourceFileName() { return sourceFileName; }
    public void setSourceFileName(String sourceFileName) { this.sourceFileName = sourceFileName; }

    public String getStdout() { return stdout; }
    public void setStdout(String stdout) { this.stdout = stdout; }

    public String getStderr() { return stderr; }
    public void setStderr(String stderr) { this.stderr = stderr; }

    public String getResultJson() { return resultJson; }
    public void setResultJson(String resultJson) { this.resultJson = resultJson; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}

