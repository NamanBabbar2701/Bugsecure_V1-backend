package com.bugsecure.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    @DBRef
    private User recipient;

    private String type; // BUG_STATUS
    private String title;
    private String message;
    private String bugReportId;

    private Boolean read = false;

    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(User recipient, String type, String title, String message, String bugReportId) {
        this.recipient = recipient;
        this.type = type;
        this.title = title;
        this.message = message;
        this.bugReportId = bugReportId;
        this.createdAt = LocalDateTime.now();
        this.read = false;
    }

    public String getId() {
        return id;
    }

    public User getRecipient() {
        return recipient;
    }

    public void setRecipient(User recipient) {
        this.recipient = recipient;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
        return read != null ? read : false;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

