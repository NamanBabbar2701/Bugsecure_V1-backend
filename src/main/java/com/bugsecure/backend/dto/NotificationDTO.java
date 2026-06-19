package com.bugsecure.backend.dto;

public class NotificationDTO {
    private String id;
    private String title;
    private String message;
    private String bugReportId;
    private Boolean read;
    private String createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getBugReportId() {
        return bugReportId;
    }

    public void setBugReportId(String bugReportId) {
        this.bugReportId = bugReportId;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

