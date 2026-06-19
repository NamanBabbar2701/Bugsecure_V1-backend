package com.bugsecure.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "sandbox_sessions")
public class SandboxSession {

    public static final String STATUS_STARTING = "STARTING";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_STOPPED = "STOPPED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    private String id;

    private String submissionId;
    private String researcherUserId;
    private String researcherUsername;

    // Docker container metadata
    private String dockerContainerName;
    private String dockerImage;
    private Integer hostPort;

    private String status = STATUS_STARTING;
    private String failureReason;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime stoppedAt;

    // Logs captured on stop (best-effort, truncated).
    private String stopLogs;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(String submissionId) {
        this.submissionId = submissionId;
    }

    public String getResearcherUserId() {
        return researcherUserId;
    }

    public void setResearcherUserId(String researcherUserId) {
        this.researcherUserId = researcherUserId;
    }

    public String getResearcherUsername() {
        return researcherUsername;
    }

    public void setResearcherUsername(String researcherUsername) {
        this.researcherUsername = researcherUsername;
    }

    public String getDockerContainerName() {
        return dockerContainerName;
    }

    public void setDockerContainerName(String dockerContainerName) {
        this.dockerContainerName = dockerContainerName;
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public void setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
    }

    public Integer getHostPort() {
        return hostPort;
    }

    public void setHostPort(Integer hostPort) {
        this.hostPort = hostPort;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getStoppedAt() {
        return stoppedAt;
    }

    public void setStoppedAt(LocalDateTime stoppedAt) {
        this.stoppedAt = stoppedAt;
    }

    public String getStopLogs() {
        return stopLogs;
    }

    public void setStopLogs(String stopLogs) {
        this.stopLogs = stopLogs;
    }
}

