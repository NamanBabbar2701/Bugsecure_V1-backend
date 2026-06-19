package com.bugsecure.backend.service;

import com.bugsecure.backend.dto.AnalyticsDTO;
import com.bugsecure.backend.model.*;
import com.bugsecure.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    @Autowired
    private CodeSubmissionRepository codeSubmissionRepository;

    @Autowired
    private BugReportRepository bugReportRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    public AnalyticsDTO getAdminAnalytics() {
        return getAdminAnalytics("monthly");
    }

    public AnalyticsDTO getAdminAnalytics(String range) {
        AnalyticsDTO analytics = new AnalyticsDTO();

        // Basic counts
        analytics.setTotalSubmissions(codeSubmissionRepository.count());
        analytics.setTotalBugReports(bugReportRepository.count());
        analytics.setTotalPayments(paymentRepository.count());

        // Submission status counts
        analytics.setOpenSubmissions((long) codeSubmissionRepository.findByStatus("OPEN").size());
        analytics.setClosedSubmissions((long) codeSubmissionRepository.findByStatus("CLOSED").size());

        // Bug report status counts
        analytics.setPendingBugReports((long) bugReportRepository.findByStatus("PENDING").size());
        analytics.setApprovedBugReports((long) bugReportRepository.findByStatus("APPROVED").size());
        analytics.setRejectedBugReports((long) bugReportRepository.findByStatus("REJECTED").size());

        // Total rewards paid
        Double totalRewards = paymentRepository.findByStatus("COMPLETED").stream()
                .mapToDouble(p -> p.getAmountUSD() != null ? p.getAmountUSD() : 0.0)
                .sum();
        analytics.setTotalRewardsPaid(totalRewards);

        // Submissions by month (last 6 months)
        analytics.setSubmissionsByMonth(getSubmissionsByMonth());

        // Phase 5 (filtered analytics)
        analytics.setEarningsByPeriod(getEarningsByPeriod(range, null));
        analytics.setBugsSubmittedVsAcceptedByPeriod(getBugsSubmittedVsAcceptedByPeriod(range, null));
        analytics.setUserEarningsPie(getUserEarningsPie());

        // Bug reports by severity
        analytics.setBugReportsBySeverity(getBugReportsBySeverity());

        // Payments by status
        analytics.setPaymentsByStatus(getPaymentsByStatus());

        // Top researchers
        analytics.setTopResearchers(getTopResearchers());

        // Top companies
        analytics.setTopCompanies(getTopCompanies());

        return analytics;
    }

    public AnalyticsDTO getCompanyAnalytics(String email) {
        return getCompanyAnalytics(email, "monthly");
    }

    public AnalyticsDTO getCompanyAnalytics(String email, String range) {
        User company = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AnalyticsDTO analytics = new AnalyticsDTO();

        List<CodeSubmission> submissions = codeSubmissionRepository.findByCompany(company);
        analytics.setTotalSubmissions((long) submissions.size());
        analytics.setOpenSubmissions(Long.valueOf(submissions.stream()
                .filter(s -> "OPEN".equals(s.getStatus())).count()));
        analytics.setClosedSubmissions(Long.valueOf(submissions.stream()
                .filter(s -> "CLOSED".equals(s.getStatus())).count()));

        List<BugReport> bugReports = new ArrayList<>();
        for (CodeSubmission submission : submissions) {
            bugReports.addAll(bugReportRepository.findByCodeSubmission(submission));
        }
        analytics.setTotalBugReports((long) bugReports.size());
        analytics.setPendingBugReports(Long.valueOf(bugReports.stream()
                .filter(br -> "PENDING".equals(br.getStatus())).count()));
        analytics.setApprovedBugReports(Long.valueOf(bugReports.stream()
                .filter(br -> "APPROVED".equals(br.getStatus())).count()));
        analytics.setRejectedBugReports(Long.valueOf(bugReports.stream()
                .filter(br -> "REJECTED".equals(br.getStatus())).count()));

        List<Payment> payments = paymentRepository.findByCompany(company);
        analytics.setTotalPayments((long) payments.size());
        Double totalRewards = payments.stream()
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .mapToDouble(p -> p.getAmountUSD() != null ? p.getAmountUSD() : 0.0)
                .sum();
        analytics.setTotalRewardsPaid(totalRewards);

        analytics.setBugReportsBySeverity(getBugReportsBySeverityForCompany(company));
        analytics.setSubmissionsByMonth(getSubmissionsByMonthForCompany(company));

        analytics.setEarningsByPeriod(getEarningsByPeriod(range, company));
        analytics.setBugsSubmittedVsAcceptedByPeriod(getBugsSubmittedVsAcceptedByPeriod(range, company));

        return analytics;
    }

    public AnalyticsDTO getResearcherAnalytics(String email) {
        return getResearcherAnalytics(email, "monthly");
    }

    public AnalyticsDTO getResearcherAnalytics(String email, String range) {
        User researcher = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        AnalyticsDTO analytics = new AnalyticsDTO();

        List<BugReport> bugReports = bugReportRepository.findByReporter(researcher);
        analytics.setTotalBugReports((long) bugReports.size());
        analytics.setPendingBugReports((long) bugReports.stream()
                .filter(br -> "PENDING".equals(br.getStatus())).count());
        analytics.setApprovedBugReports((long) bugReports.stream()
                .filter(br -> "APPROVED".equals(br.getStatus())).count());
        analytics.setRejectedBugReports((long) bugReports.stream()
                .filter(br -> "REJECTED".equals(br.getStatus())).count());

        List<Payment> payments = paymentRepository.findByResearcher(researcher);
        analytics.setTotalPayments((long) payments.size());
        Double totalRewards = payments.stream()
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .mapToDouble(p -> p.getAmountUSD() != null ? p.getAmountUSD() : 0.0)
                .sum();
        analytics.setTotalRewardsPaid(totalRewards);

        analytics.setBugReportsBySeverity(getBugReportsBySeverityForResearcher(researcher));

        analytics.setEarningsByPeriod(getEarningsByPeriod(range, null, researcher));
        analytics.setBugsSubmittedVsAcceptedByPeriod(getBugsSubmittedVsAcceptedByPeriod(range, null, researcher));
        analytics.setUserEarningsPie(getUserEarningsPieForResearcher(researcher));

        return analytics;
    }

    private List<Map<String, Object>> getSubmissionsByMonth() {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1);
            
            long count = codeSubmissionRepository.findAll().stream()
                    .filter(s -> s.getCreatedAt().isAfter(monthStart) && s.getCreatedAt().isBefore(monthEnd))
                    .count();
            
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", monthStart.format(DateTimeFormatter.ofPattern("MMM yyyy")));
            monthData.put("count", count);
            result.add(monthData);
        }
        
        return result;
    }

    private List<Map<String, Object>> getEarningsByPeriod(String range, User companyScope) {
        return getEarningsByPeriod(range, companyScope, null);
    }

    private List<Map<String, Object>> getEarningsByPeriod(String range, User companyScope, User researcherScope) {
        List<Payment> payments = paymentRepository.findByStatus("COMPLETED");
        if (companyScope != null) {
            payments = payments.stream()
                    .filter(p -> p.getCompany() != null && p.getCompany().getId() != null && p.getCompany().getId().equals(companyScope.getId()))
                    .collect(Collectors.toList());
        }
        if (researcherScope != null) {
            payments = payments.stream()
                    .filter(p -> p.getResearcher() != null && p.getResearcher().getId() != null && p.getResearcher().getId().equals(researcherScope.getId()))
                    .collect(Collectors.toList());
        }

        List<LocalDateTime[]> buckets = getPeriodBuckets(range);
        List<Map<String, Object>> result = new ArrayList<>();

        for (LocalDateTime[] bucket : buckets) {
            LocalDateTime start = bucket[0];
            LocalDateTime end = bucket[1];
            double sum = payments.stream()
                    .filter(p -> p.getCreatedAt() != null && (p.getCreatedAt().isEqual(start) || p.getCreatedAt().isAfter(start)))
                    .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isBefore(end))
                    .mapToDouble(p -> p.getAmountUSD() != null ? p.getAmountUSD() : 0.0)
                    .sum();

            Map<String, Object> item = new HashMap<>();
            item.put("period", formatBucketLabel(start, range));
            item.put("earningsUSD", sum);
            result.add(item);
        }

        return result;
    }

    private List<Map<String, Object>> getBugsSubmittedVsAcceptedByPeriod(String range, User companyScope) {
        return getBugsSubmittedVsAcceptedByPeriod(range, companyScope, null);
    }

    private List<Map<String, Object>> getBugsSubmittedVsAcceptedByPeriod(String range, User companyScope, User researcherScope) {
        List<BugReport> allReports = bugReportRepository.findAll();
        if (companyScope != null) {
            allReports = allReports.stream()
                    .filter(br -> br.getCodeSubmission() != null && br.getCodeSubmission().getCompany() != null)
                    .filter(br -> br.getCodeSubmission().getCompany().getId() != null && br.getCodeSubmission().getCompany().getId().equals(companyScope.getId()))
                    .collect(Collectors.toList());
        }
        if (researcherScope != null) {
            allReports = allReports.stream()
                    .filter(br -> br.getReporter() != null && br.getReporter().getId() != null && br.getReporter().getId().equals(researcherScope.getId()))
                    .collect(Collectors.toList());
        }

        List<LocalDateTime[]> buckets = getPeriodBuckets(range);
        List<Map<String, Object>> result = new ArrayList<>();

        for (LocalDateTime[] bucket : buckets) {
            LocalDateTime start = bucket[0];
            LocalDateTime end = bucket[1];

            long submitted = allReports.stream()
                    .filter(br -> br.getCreatedAt() != null && !br.getCreatedAt().isBefore(start))
                    .filter(br -> br.getCreatedAt() != null && br.getCreatedAt().isBefore(end))
                    .count();

            long accepted = allReports.stream()
                    .filter(br -> "APPROVED".equalsIgnoreCase(br.getStatus()))
                    .filter(br -> br.getUpdatedAt() != null && !br.getUpdatedAt().isBefore(start))
                    .filter(br -> br.getUpdatedAt() != null && br.getUpdatedAt().isBefore(end))
                    .count();

            Map<String, Object> item = new HashMap<>();
            item.put("period", formatBucketLabel(start, range));
            item.put("submittedCount", submitted);
            item.put("acceptedCount", accepted);
            result.add(item);
        }

        return result;
    }

    private List<Map<String, Object>> getUserEarningsPie() {
        // Use top researchers data as "user earnings" distribution.
        // (Front-end will render as pie.)
        List<Map<String, Object>> top = getTopResearchers();
        return top.stream()
                .map(t -> Map.of(
                        "name", t.get("name"),
                        "earningsUSD", t.get("earnings")
                ))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getUserEarningsPieForResearcher(User researcher) {
        // Simple pie: researcher gets a single slice (100%).
        List<Payment> completed = paymentRepository.findByResearcher(researcher).stream()
                .filter(p -> "COMPLETED".equals(p.getStatus()))
                .collect(Collectors.toList());

        double sum = completed.stream().mapToDouble(p -> p.getAmountUSD() != null ? p.getAmountUSD() : 0.0).sum();
        return List.of(Map.of("name", researcher.getUsername(), "earningsUSD", sum));
    }

    private List<LocalDateTime[]> getPeriodBuckets(String range) {
        String normalized = range == null ? "monthly" : range.toLowerCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();
        List<LocalDateTime[]> buckets = new ArrayList<>();

        if ("weekly".equals(normalized)) {
            // Last 8 weeks
            for (int i = 7; i >= 0; i--) {
                LocalDateTime start = now.minusWeeks(i);
                // Use Monday as week start
                start = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .withHour(0).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime end = start.plusWeeks(1);
                buckets.add(new LocalDateTime[]{start, end});
            }
        } else if ("yearly".equals(normalized)) {
            // Last 5 years
            for (int i = 4; i >= 0; i--) {
                LocalDateTime start = now.minusYears(i).withDayOfYear(1)
                        .withHour(0).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime end = start.plusYears(1);
                buckets.add(new LocalDateTime[]{start, end});
            }
        } else {
            // Default: monthly (last 12 months)
            for (int i = 11; i >= 0; i--) {
                LocalDateTime start = now.minusMonths(i).withDayOfMonth(1)
                        .withHour(0).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime end = start.plusMonths(1);
                buckets.add(new LocalDateTime[]{start, end});
            }
        }

        return buckets;
    }

    private String formatBucketLabel(LocalDateTime start, String range) {
        String normalized = range == null ? "monthly" : range.toLowerCase(Locale.ROOT);
        if ("weekly".equals(normalized)) {
            return start.format(DateTimeFormatter.ofPattern("MMM d"));
        }
        if ("yearly".equals(normalized)) {
            return start.format(DateTimeFormatter.ofPattern("yyyy"));
        }
        return start.format(DateTimeFormatter.ofPattern("MMM yyyy"));
    }

    private List<Map<String, Object>> getSubmissionsByMonthForCompany(User company) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        List<CodeSubmission> submissions = codeSubmissionRepository.findByCompany(company);
        
        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1);
            
            long count = submissions.stream()
                    .filter(s -> s.getCreatedAt().isAfter(monthStart) && s.getCreatedAt().isBefore(monthEnd))
                    .count();
            
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", monthStart.format(DateTimeFormatter.ofPattern("MMM yyyy")));
            monthData.put("count", count);
            result.add(monthData);
        }
        
        return result;
    }

    private List<Map<String, Object>> getBugReportsBySeverity() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<BugReport> allReports = bugReportRepository.findAll();
        
        Map<String, Long> severityCounts = allReports.stream()
                .collect(Collectors.groupingBy(BugReport::getSeverity, Collectors.counting()));
        
        for (String severity : Arrays.asList("CRITICAL", "HIGH", "MEDIUM", "LOW")) {
            Map<String, Object> severityData = new HashMap<>();
            severityData.put("severity", severity);
            severityData.put("count", severityCounts.getOrDefault(severity, 0L));
            result.add(severityData);
        }
        
        return result;
    }

    private List<Map<String, Object>> getBugReportsBySeverityForCompany(User company) {
        List<CodeSubmission> submissions = codeSubmissionRepository.findByCompany(company);
        List<BugReport> bugReports = new ArrayList<>();
        for (CodeSubmission submission : submissions) {
            bugReports.addAll(bugReportRepository.findByCodeSubmission(submission));
        }
        
        Map<String, Long> severityCounts = bugReports.stream()
                .collect(Collectors.groupingBy(BugReport::getSeverity, Collectors.counting()));
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (String severity : Arrays.asList("CRITICAL", "HIGH", "MEDIUM", "LOW")) {
            Map<String, Object> severityData = new HashMap<>();
            severityData.put("severity", severity);
            severityData.put("count", severityCounts.getOrDefault(severity, 0L));
            result.add(severityData);
        }
        
        return result;
    }

    private List<Map<String, Object>> getBugReportsBySeverityForResearcher(User researcher) {
        List<BugReport> bugReports = bugReportRepository.findByReporter(researcher);
        
        Map<String, Long> severityCounts = bugReports.stream()
                .collect(Collectors.groupingBy(BugReport::getSeverity, Collectors.counting()));
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (String severity : Arrays.asList("CRITICAL", "HIGH", "MEDIUM", "LOW")) {
            Map<String, Object> severityData = new HashMap<>();
            severityData.put("severity", severity);
            severityData.put("count", severityCounts.getOrDefault(severity, 0L));
            result.add(severityData);
        }
        
        return result;
    }

    private List<Map<String, Object>> getPaymentsByStatus() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Payment> allPayments = paymentRepository.findAll();
        
        Map<String, Long> statusCounts = allPayments.stream()
                .collect(Collectors.groupingBy(Payment::getStatus, Collectors.counting()));
        
        for (String status : Arrays.asList("PENDING", "PROCESSING", "COMPLETED", "FAILED")) {
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("status", status);
            statusData.put("count", statusCounts.getOrDefault(status, 0L));
            result.add(statusData);
        }
        
        return result;
    }

    private List<Map<String, Object>> getTopResearchers() {
        List<Payment> completedPayments = paymentRepository.findByStatus("COMPLETED");
        Map<User, Double> researcherEarnings = completedPayments.stream()
                .collect(Collectors.groupingBy(
                    Payment::getResearcher,
                    Collectors.summingDouble(p -> p.getAmountUSD() != null ? p.getAmountUSD() : 0.0)
                ));
        
        return researcherEarnings.entrySet().stream()
                .sorted(Map.Entry.<User, Double>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> researcherData = new HashMap<>();
                    researcherData.put("name", entry.getKey().getUsername());
                    researcherData.put("earnings", entry.getValue());
                    researcherData.put("count", completedPayments.stream()
                            .filter(p -> p.getResearcher().getId().equals(entry.getKey().getId()))
                            .count());
                    return researcherData;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getTopCompanies() {
        List<CodeSubmission> allSubmissions = codeSubmissionRepository.findAll();
        Map<User, Long> companySubmissions = allSubmissions.stream()
                .collect(Collectors.groupingBy(
                    CodeSubmission::getCompany,
                    Collectors.counting()
                ));
        
        return companySubmissions.entrySet().stream()
                .sorted(Map.Entry.<User, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> companyData = new HashMap<>();
                    companyData.put("name", entry.getKey().getCompanyName() != null ? 
                                  entry.getKey().getCompanyName() : entry.getKey().getUsername());
                    companyData.put("submissions", entry.getValue());
                    return companyData;
                })
                .collect(Collectors.toList());
    }
}

