package com.bugsecure.backend.dto;

import java.util.List;
import java.util.Map;

public class AnalyticsDTO {
    private Long totalSubmissions;
    private Long totalBugReports;
    private Long totalPayments;
    private Double totalRewardsPaid;
    private Long openSubmissions;
    private Long closedSubmissions;
    private Long pendingBugReports;
    private Long approvedBugReports;
    private Long rejectedBugReports;
    private List<Map<String, Object>> submissionsByMonth;
    private List<Map<String, Object>> bugReportsBySeverity;
    private List<Map<String, Object>> paymentsByStatus;
    private List<Map<String, Object>> topResearchers;
    private List<Map<String, Object>> topCompanies;

    // Phase 5 (filtered analytics)
    private List<Map<String, Object>> earningsByPeriod; // Line chart: money added over time
    private List<Map<String, Object>> bugsSubmittedVsAcceptedByPeriod; // Bar chart: submitted vs accepted
    private List<Map<String, Object>> userEarningsPie; // Pie chart: user earnings distribution

    // Getters and Setters
    public Long getTotalSubmissions() {
        return totalSubmissions;
    }

    public void setTotalSubmissions(Long totalSubmissions) {
        this.totalSubmissions = totalSubmissions;
    }

    public Long getTotalBugReports() {
        return totalBugReports;
    }

    public void setTotalBugReports(Long totalBugReports) {
        this.totalBugReports = totalBugReports;
    }

    public Long getTotalPayments() {
        return totalPayments;
    }

    public void setTotalPayments(Long totalPayments) {
        this.totalPayments = totalPayments;
    }

    public Double getTotalRewardsPaid() {
        return totalRewardsPaid;
    }

    public void setTotalRewardsPaid(Double totalRewardsPaid) {
        this.totalRewardsPaid = totalRewardsPaid;
    }

    public Long getOpenSubmissions() {
        return openSubmissions;
    }

    public void setOpenSubmissions(Long openSubmissions) {
        this.openSubmissions = openSubmissions;
    }

    public Long getClosedSubmissions() {
        return closedSubmissions;
    }

    public void setClosedSubmissions(Long closedSubmissions) {
        this.closedSubmissions = closedSubmissions;
    }

    public Long getPendingBugReports() {
        return pendingBugReports;
    }

    public void setPendingBugReports(Long pendingBugReports) {
        this.pendingBugReports = pendingBugReports;
    }

    public Long getApprovedBugReports() {
        return approvedBugReports;
    }

    public void setApprovedBugReports(Long approvedBugReports) {
        this.approvedBugReports = approvedBugReports;
    }

    public Long getRejectedBugReports() {
        return rejectedBugReports;
    }

    public void setRejectedBugReports(Long rejectedBugReports) {
        this.rejectedBugReports = rejectedBugReports;
    }

    public List<Map<String, Object>> getSubmissionsByMonth() {
        return submissionsByMonth;
    }

    public void setSubmissionsByMonth(List<Map<String, Object>> submissionsByMonth) {
        this.submissionsByMonth = submissionsByMonth;
    }

    public List<Map<String, Object>> getBugReportsBySeverity() {
        return bugReportsBySeverity;
    }

    public void setBugReportsBySeverity(List<Map<String, Object>> bugReportsBySeverity) {
        this.bugReportsBySeverity = bugReportsBySeverity;
    }

    public List<Map<String, Object>> getPaymentsByStatus() {
        return paymentsByStatus;
    }

    public void setPaymentsByStatus(List<Map<String, Object>> paymentsByStatus) {
        this.paymentsByStatus = paymentsByStatus;
    }

    public List<Map<String, Object>> getTopResearchers() {
        return topResearchers;
    }

    public void setTopResearchers(List<Map<String, Object>> topResearchers) {
        this.topResearchers = topResearchers;
    }

    public List<Map<String, Object>> getTopCompanies() {
        return topCompanies;
    }

    public void setTopCompanies(List<Map<String, Object>> topCompanies) {
        this.topCompanies = topCompanies;
    }

    public List<Map<String, Object>> getEarningsByPeriod() {
        return earningsByPeriod;
    }

    public void setEarningsByPeriod(List<Map<String, Object>> earningsByPeriod) {
        this.earningsByPeriod = earningsByPeriod;
    }

    public List<Map<String, Object>> getBugsSubmittedVsAcceptedByPeriod() {
        return bugsSubmittedVsAcceptedByPeriod;
    }

    public void setBugsSubmittedVsAcceptedByPeriod(List<Map<String, Object>> bugsSubmittedVsAcceptedByPeriod) {
        this.bugsSubmittedVsAcceptedByPeriod = bugsSubmittedVsAcceptedByPeriod;
    }

    public List<Map<String, Object>> getUserEarningsPie() {
        return userEarningsPie;
    }

    public void setUserEarningsPie(List<Map<String, Object>> userEarningsPie) {
        this.userEarningsPie = userEarningsPie;
    }
}













