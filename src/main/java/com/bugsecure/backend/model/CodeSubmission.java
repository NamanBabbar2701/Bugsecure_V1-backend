package com.bugsecure.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "code_submissions")
public class CodeSubmission {

    @Id
    private String id;
    private String title;
    private String description;
    private String fileName; // Optional - can be empty if code is pasted directly
    // Legacy: persisted code content for older records.
    private String codeContent;

    // Secure storage for code bytes. New submissions should prefer these.
    private String codeStorageKey;
    private String codeMimeType; // e.g., text/plain
    private Long codeSizeBytes;
    private String status = "OPEN"; // OPEN, IN_PROGRESS, CLOSED
    private Double rewardAmount;
    private String website; // Website URL for testing
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // =============================
    // Bug bounty program settings
    // =============================
    // Scope
    private List<String> inScopeTargets;
    private List<String> outOfScopeTargets;

    // Allowed / restricted testing
    private List<String> allowedTestingTypes;
    private List<String> restrictedActions;

    // Reward structure (critical is mapped to `rewardAmount` for compatibility)
    private Double rewardLowSeverity;
    private Double rewardMediumSeverity;
    private Double rewardHighSeverity;
    private Double rewardCriticalSeverity;

    private String environmentSetting; // Staging / Production / Code Only
    private LocalDateTime programStartAt;
    private LocalDateTime programEndAt;
    private String accessControl; // Public / Invite Only / Limited Researchers

    // Testing credentials (optional)
    private String testingCredentialsEmail;
    // For production, store a password hash (not plaintext). UI may send plaintext for now.
    private String testingCredentialsPasswordHash;

    // Legal agreement
    private Boolean agreedDisclosure;
    private Boolean agreedNoHarm;
    
    @DBRef
    private User company;

    // Constructors
    public CodeSubmission() {
    }

    public CodeSubmission(String title, String description, String fileName, 
                         String codeContent, Double rewardAmount, User company) {
        this.title = title;
        this.description = description;
        this.fileName = fileName;
        this.codeContent = codeContent;
        this.rewardAmount = rewardAmount;
        this.company = company;
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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getCodeContent() {
        return codeContent;
    }

    public void setCodeContent(String codeContent) {
        this.codeContent = codeContent;
    }

    public String getCodeStorageKey() {
        return codeStorageKey;
    }

    public void setCodeStorageKey(String codeStorageKey) {
        this.codeStorageKey = codeStorageKey;
    }

    public String getCodeMimeType() {
        return codeMimeType;
    }

    public void setCodeMimeType(String codeMimeType) {
        this.codeMimeType = codeMimeType;
    }

    public Long getCodeSizeBytes() {
        return codeSizeBytes;
    }

    public void setCodeSizeBytes(Long codeSizeBytes) {
        this.codeSizeBytes = codeSizeBytes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Double getRewardAmount() {
        return rewardAmount;
    }

    public void setRewardAmount(Double rewardAmount) {
        this.rewardAmount = rewardAmount;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
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

    public List<String> getInScopeTargets() {
        return inScopeTargets;
    }

    public void setInScopeTargets(List<String> inScopeTargets) {
        this.inScopeTargets = inScopeTargets;
    }

    public List<String> getOutOfScopeTargets() {
        return outOfScopeTargets;
    }

    public void setOutOfScopeTargets(List<String> outOfScopeTargets) {
        this.outOfScopeTargets = outOfScopeTargets;
    }

    public List<String> getAllowedTestingTypes() {
        return allowedTestingTypes;
    }

    public void setAllowedTestingTypes(List<String> allowedTestingTypes) {
        this.allowedTestingTypes = allowedTestingTypes;
    }

    public List<String> getRestrictedActions() {
        return restrictedActions;
    }

    public void setRestrictedActions(List<String> restrictedActions) {
        this.restrictedActions = restrictedActions;
    }

    public Double getRewardLowSeverity() {
        return rewardLowSeverity;
    }

    public void setRewardLowSeverity(Double rewardLowSeverity) {
        this.rewardLowSeverity = rewardLowSeverity;
    }

    public Double getRewardMediumSeverity() {
        return rewardMediumSeverity;
    }

    public void setRewardMediumSeverity(Double rewardMediumSeverity) {
        this.rewardMediumSeverity = rewardMediumSeverity;
    }

    public Double getRewardHighSeverity() {
        return rewardHighSeverity;
    }

    public void setRewardHighSeverity(Double rewardHighSeverity) {
        this.rewardHighSeverity = rewardHighSeverity;
    }

    public Double getRewardCriticalSeverity() {
        return rewardCriticalSeverity;
    }

    public void setRewardCriticalSeverity(Double rewardCriticalSeverity) {
        this.rewardCriticalSeverity = rewardCriticalSeverity;
    }

    public String getEnvironmentSetting() {
        return environmentSetting;
    }

    public void setEnvironmentSetting(String environmentSetting) {
        this.environmentSetting = environmentSetting;
    }

    public LocalDateTime getProgramStartAt() {
        return programStartAt;
    }

    public void setProgramStartAt(LocalDateTime programStartAt) {
        this.programStartAt = programStartAt;
    }

    public LocalDateTime getProgramEndAt() {
        return programEndAt;
    }

    public void setProgramEndAt(LocalDateTime programEndAt) {
        this.programEndAt = programEndAt;
    }

    public String getAccessControl() {
        return accessControl;
    }

    public void setAccessControl(String accessControl) {
        this.accessControl = accessControl;
    }

    public String getTestingCredentialsEmail() {
        return testingCredentialsEmail;
    }

    public void setTestingCredentialsEmail(String testingCredentialsEmail) {
        this.testingCredentialsEmail = testingCredentialsEmail;
    }

    public String getTestingCredentialsPasswordHash() {
        return testingCredentialsPasswordHash;
    }

    public void setTestingCredentialsPasswordHash(String testingCredentialsPasswordHash) {
        this.testingCredentialsPasswordHash = testingCredentialsPasswordHash;
    }

    public Boolean getAgreedDisclosure() {
        return agreedDisclosure;
    }

    public void setAgreedDisclosure(Boolean agreedDisclosure) {
        this.agreedDisclosure = agreedDisclosure;
    }

    public Boolean getAgreedNoHarm() {
        return agreedNoHarm;
    }

    public void setAgreedNoHarm(Boolean agreedNoHarm) {
        this.agreedNoHarm = agreedNoHarm;
    }

    public User getCompany() {
        return company;
    }

    public void setCompany(User company) {
        this.company = company;
    }
}

