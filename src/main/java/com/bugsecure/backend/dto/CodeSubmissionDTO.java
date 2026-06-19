package com.bugsecure.backend.dto;

import java.util.List;
import java.util.Map;

public class CodeSubmissionDTO {
    private String id;
    private String title;
    private String description;
    private String fileName;
    private String codeContent;
    private String status;
    private Double rewardAmount;
    private String createdAt;
    private String companyName;
    private String companyId;
    private String website; // Website URL for testing
    private List<Map<String, Object>> files; // List of file objects

    // =============================
    // Bug bounty program settings
    // =============================
    private List<String> inScopeTargets;
    private List<String> outOfScopeTargets;
    private List<String> allowedTestingTypes;
    private List<String> restrictedActions;

    private Double rewardLowSeverity;
    private Double rewardMediumSeverity;
    private Double rewardHighSeverity;
    private Double rewardCriticalSeverity;

    private String environmentSetting;
    private String startDate; // YYYY-MM-DD from UI
    private String endDate; // YYYY-MM-DD from UI
    private String accessControl;

    private String testingEmail;
    private String testingPassword;

    private Boolean agreedDisclosure;
    private Boolean agreedNoHarm;

    // Constructors
    public CodeSubmissionDTO() {
    }

    // Getters and Setters
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public List<Map<String, Object>> getFiles() {
        return files;
    }

    public void setFiles(List<Map<String, Object>> files) {
        this.files = files;
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

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getAccessControl() {
        return accessControl;
    }

    public void setAccessControl(String accessControl) {
        this.accessControl = accessControl;
    }

    public String getTestingEmail() {
        return testingEmail;
    }

    public void setTestingEmail(String testingEmail) {
        this.testingEmail = testingEmail;
    }

    public String getTestingPassword() {
        return testingPassword;
    }

    public void setTestingPassword(String testingPassword) {
        this.testingPassword = testingPassword;
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
}

