package com.bugsecure.backend.dto;

public class PaymentDTO {
    private String id;
    private Double amountUSD;
    private Double amountINR;
    private String status;
    private String paymentMethod;
    private String transactionId;
    private String notes;
    private String createdAt;
    private String bugReportId;
    private String bugReportTitle;
    private String companyId;
    private String companyName;
    private String researcherId;
    private String researcherName;

    // Constructors
    public PaymentDTO() {
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Double getAmountUSD() {
        return amountUSD;
    }

    public void setAmountUSD(Double amountUSD) {
        this.amountUSD = amountUSD;
    }

    public Double getAmountINR() {
        return amountINR;
    }

    public void setAmountINR(Double amountINR) {
        this.amountINR = amountINR;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getBugReportId() {
        return bugReportId;
    }

    public void setBugReportId(String bugReportId) {
        this.bugReportId = bugReportId;
    }

    public String getBugReportTitle() {
        return bugReportTitle;
    }

    public void setBugReportTitle(String bugReportTitle) {
        this.bugReportTitle = bugReportTitle;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getResearcherId() {
        return researcherId;
    }

    public void setResearcherId(String researcherId) {
        this.researcherId = researcherId;
    }

    public String getResearcherName() {
        return researcherName;
    }

    public void setResearcherName(String researcherName) {
        this.researcherName = researcherName;
    }
}







