package com.bugsecure.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.LocalDateTime;

@Document(collection = "bug_reports")
public class BugReport {

    @Id
    private String id;
    private String title;
    private String description;
    private String stepsToReproduce;
    private String expectedBehavior;
    private String actualBehavior;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, FIXED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @DBRef
    private CodeSubmission codeSubmission;
    
    @DBRef
    private User reporter;
    
    private Double rewardAmount;

    // Gross bounty amount (company debited) in USD, before platform commission is deducted.
    private Double bountyAmountUSD;

    // Platform commission amount in USD (admin/platform share) for this approved bug report.
    private Double platformCommissionAmountUSD;

    // Net amount in USD credited to the researcher (bounty - commission).
    private Double researcherNetAmountUSD;

    // Prevent double-crediting wallet entries if APPROVED is processed more than once.
    private Boolean walletSplitCompleted = false;

    // Constructors
    public BugReport() {
    }

    public BugReport(String title, String description, String stepsToReproduce,
                    String expectedBehavior, String actualBehavior, String severity,
                    CodeSubmission codeSubmission, User reporter) {
        this.title = title;
        this.description = description;
        this.stepsToReproduce = stepsToReproduce;
        this.expectedBehavior = expectedBehavior;
        this.actualBehavior = actualBehavior;
        this.severity = severity;
        this.codeSubmission = codeSubmission;
        this.reporter = reporter;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public void setCreatedAtIfNew() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStepsToReproduce() {
        return stepsToReproduce;
    }

    public void setStepsToReproduce(String stepsToReproduce) {
        this.stepsToReproduce = stepsToReproduce;
    }

    public String getExpectedBehavior() {
        return expectedBehavior;
    }

    public void setExpectedBehavior(String expectedBehavior) {
        this.expectedBehavior = expectedBehavior;
    }

    public String getActualBehavior() {
        return actualBehavior;
    }

    public void setActualBehavior(String actualBehavior) {
        this.actualBehavior = actualBehavior;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public CodeSubmission getCodeSubmission() {
        return codeSubmission;
    }

    public void setCodeSubmission(CodeSubmission codeSubmission) {
        this.codeSubmission = codeSubmission;
    }

    public User getReporter() {
        return reporter;
    }

    public void setReporter(User reporter) {
        this.reporter = reporter;
    }

    public Double getRewardAmount() {
        return rewardAmount;
    }

    public void setRewardAmount(Double rewardAmount) {
        this.rewardAmount = rewardAmount;
    }

    public Double getBountyAmountUSD() {
        return bountyAmountUSD;
    }

    public void setBountyAmountUSD(Double bountyAmountUSD) {
        this.bountyAmountUSD = bountyAmountUSD;
    }

    public Double getPlatformCommissionAmountUSD() {
        return platformCommissionAmountUSD;
    }

    public void setPlatformCommissionAmountUSD(Double platformCommissionAmountUSD) {
        this.platformCommissionAmountUSD = platformCommissionAmountUSD;
    }

    public Double getResearcherNetAmountUSD() {
        return researcherNetAmountUSD;
    }

    public void setResearcherNetAmountUSD(Double researcherNetAmountUSD) {
        this.researcherNetAmountUSD = researcherNetAmountUSD;
    }

    public Boolean getWalletSplitCompleted() {
        return walletSplitCompleted != null ? walletSplitCompleted : false;
    }

    public void setWalletSplitCompleted(Boolean walletSplitCompleted) {
        this.walletSplitCompleted = walletSplitCompleted;
    }
}

