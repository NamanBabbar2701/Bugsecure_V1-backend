package com.bugsecure.backend.service;

import com.bugsecure.backend.dto.BugReportDTO;
import com.bugsecure.backend.model.BugReport;
import com.bugsecure.backend.model.CodeSubmission;
import com.bugsecure.backend.model.User;
import com.bugsecure.backend.repository.BugReportRepository;
import com.bugsecure.backend.repository.CodeSubmissionRepository;
import com.bugsecure.backend.repository.UserRepository;
import com.bugsecure.backend.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BugReportService {

    @Autowired
    private BugReportRepository bugReportRepository;

    @Autowired
    private CodeSubmissionRepository codeSubmissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletService walletService;

    @Autowired
    private NotificationService notificationService;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Platform takes 5% commission from the gross bounty.
    private static final double PLATFORM_COMMISSION_RATE = 0.05;

    public BugReportDTO createBugReport(BugReportDTO dto, String email) {
        User reporter = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("COMPANY".equals(reporter.getRole())) {
            throw new RuntimeException("Companies cannot submit bug reports");
        }

        CodeSubmission submission = codeSubmissionRepository.findById(dto.getSubmissionId())
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        BugReport bugReport = new BugReport();
        bugReport.setTitle(dto.getTitle());
        bugReport.setDescription(dto.getDescription());
        bugReport.setStepsToReproduce(dto.getStepsToReproduce());
        bugReport.setExpectedBehavior(dto.getExpectedBehavior());
        bugReport.setActualBehavior(dto.getActualBehavior());
        bugReport.setSeverity(dto.getSeverity());
        bugReport.setCodeSubmission(submission);
        bugReport.setReporter(reporter);
        bugReport.setStatus("PENDING");
        bugReport.setCreatedAtIfNew(); // Set timestamps for MongoDB

        BugReport saved = bugReportRepository.save(bugReport);
        return convertToDTO(saved);
    }

    public List<BugReportDTO> getAllBugReports() {
        return bugReportRepository.findAll().stream()
                .map(br -> convertToDTOForViewer(br, "ADMIN"))
                .collect(Collectors.toList());
    }

    public List<BugReportDTO> getBugReportsByReporter(String email) {
        User reporter = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return bugReportRepository.findByReporter(reporter).stream()
                .map(br -> convertToDTOForViewer(br, "USER"))
                .collect(Collectors.toList());
    }

    public List<BugReportDTO> getBugReportsBySubmission(String submissionId) {
        CodeSubmission submission = codeSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        return bugReportRepository.findByCodeSubmission(submission).stream()
                .map(br -> convertToDTOForViewer(br, "COMPANY"))
                .collect(Collectors.toList());
    }

    public List<BugReportDTO> getBugReportsBySubmission(String submissionId, String viewerEmail, String viewerRole) {
        CodeSubmission submission;

        if ("COMPANY".equals(viewerRole)) {
            User company = userRepository.findByEmail(viewerEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            submission = codeSubmissionRepository.findByIdAndCompany(submissionId, company)
                    .orElseThrow(() -> new RuntimeException("Submission not found or unauthorized"));
        } else if ("ADMIN".equals(viewerRole)) {
            submission = codeSubmissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("Submission not found"));
        } else {
            throw new RuntimeException("Unauthorized to view this submission");
        }

        return bugReportRepository.findByCodeSubmission(submission).stream()
                .map(br -> convertToDTOForViewer(br, viewerRole))
                .collect(Collectors.toList());
    }

    public BugReportDTO getBugReportById(String id) {
        BugReport bugReport = bugReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bug report not found"));
        // Default to net view; endpoints should pass viewer role if needed.
        return convertToDTOForViewer(bugReport, "USER");
    }

    public BugReportDTO updateBugReportStatus(String id, String status, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BugReport bugReport = bugReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bug report not found"));

        // Only company owner or admin can update status
        if (!"ADMIN".equals(user.getRole()) && 
            !bugReport.getCodeSubmission().getCompany().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized to update bug report status");
        }

        String oldStatus = bugReport.getStatus();
        bugReport.setStatus(status);
        
        // If approved, set reward amount based on severity
        if ("APPROVED".equals(status)) {
            // Idempotency guard: wallet split must only happen once per bug report.
            if (!bugReport.getWalletSplitCompleted()) {
                Double baseReward = bugReport.getCodeSubmission().getRewardAmount();
                double multiplier = getSeverityMultiplier(bugReport.getSeverity());

                // Business rule:
                // - Gross bounty paid by company depends on severity.
                // - Platform commission is always 5% of the *base* bounty (company rewardAmount),
                //   independent of severity.
                // Example: base=300, severity=LOW(mult=0.25) => gross=75, commission=15, net=60.
                Double grossBountyUSD = (baseReward != null ? baseReward : 0.0) * multiplier;
                Double commissionUSD = (baseReward != null ? baseReward : 0.0) * PLATFORM_COMMISSION_RATE;
                // Researcher net = gross - commission
                Double netToResearcherUSD = grossBountyUSD - commissionUSD;

                bugReport.setBountyAmountUSD(grossBountyUSD);
                bugReport.setPlatformCommissionAmountUSD(commissionUSD);
                bugReport.setResearcherNetAmountUSD(netToResearcherUSD);

                // Keep existing field for backwards compatibility (net for researcher views).
                bugReport.setRewardAmount(netToResearcherUSD);
            }
        }
        
        bugReport.updateTimestamp(); // Update timestamp for MongoDB

        BugReport updated = bugReportRepository.save(bugReport);

        // Notify reporter + company on actual status change
        if (oldStatus != null && status != null && !oldStatus.equals(status)) {
            try {
                User reporter = updated.getReporter();
                User company = updated.getCodeSubmission() != null ? updated.getCodeSubmission().getCompany() : null;

                String title = "Bug report update";
                String msg = String.format("'%s' is now %s.", updated.getTitle(), updated.getStatus());

                notificationService.createBugStatusNotification(reporter, title, msg, updated.getId());
                notificationService.createBugStatusNotification(company, title, msg, updated.getId());
            } catch (Exception ignored) {
                // Notifications should never block the core status update.
            }
        }

        // If status transitioned to APPROVED, perform the 3-way split:
        // company debit (gross), researcher credit (net), platform commission (5%)
        if ("APPROVED".equals(status) && !"APPROVED".equals(oldStatus) && !updated.getWalletSplitCompleted()) {
            try {
                User company = updated.getCodeSubmission().getCompany();
                User researcher = updated.getReporter();

                Double grossBountyUSD = updated.getBountyAmountUSD() != null ? updated.getBountyAmountUSD() : updated.getRewardAmount();
                Double commissionUSD = updated.getPlatformCommissionAmountUSD() != null ? updated.getPlatformCommissionAmountUSD() : 0.0;
                Double netToResearcherUSD = updated.getResearcherNetAmountUSD() != null ? updated.getResearcherNetAmountUSD() : updated.getRewardAmount();

                if (grossBountyUSD != null && grossBountyUSD > 0 && netToResearcherUSD != null && netToResearcherUSD >= 0) {
                    walletService.transferBountyWithCommissionToResearcher(
                            company.getId(),
                            researcher.getId(),
                            grossBountyUSD,
                            netToResearcherUSD,
                            commissionUSD,
                            null, // use configured platform admin email
                            String.format("Bug bounty reward (net) for: %s", updated.getTitle()),
                            String.format("Bounty paid for: %s", updated.getTitle()),
                            String.format("BugSecure commission for: %s", updated.getTitle())
                    );

                    // Mark split as completed after successful transfer.
                    updated.setWalletSplitCompleted(true);
                    bugReportRepository.save(updated);
                }
            } catch (Exception e) {
                // Do not fail the status update if split transfer fails.
                System.err.println("Failed to perform bounty split: " + e.getMessage());
            }
        }

        return convertToDTOForViewer(updated, user.getRole());
    }

    private double getSeverityMultiplier(String severity) {
        switch (severity.toUpperCase()) {
            case "CRITICAL": return 1.0;
            case "HIGH": return 0.75;
            case "MEDIUM": return 0.5;
            case "LOW": return 0.25;
            default: return 0.5;
        }
    }

    private BugReportDTO convertToDTOForViewer(BugReport bugReport, String viewerRole) {
        BugReportDTO dto = new BugReportDTO();
        dto.setId(bugReport.getId());
        dto.setTitle(bugReport.getTitle());
        dto.setDescription(bugReport.getDescription());
        dto.setStepsToReproduce(bugReport.getStepsToReproduce());
        dto.setExpectedBehavior(bugReport.getExpectedBehavior());
        dto.setActualBehavior(bugReport.getActualBehavior());
        dto.setSeverity(bugReport.getSeverity());
        dto.setStatus(bugReport.getStatus());
        dto.setCreatedAt(bugReport.getCreatedAt().format(formatter));
        dto.setSubmissionId(bugReport.getCodeSubmission().getId());
        dto.setSubmissionTitle(bugReport.getCodeSubmission().getTitle());
        dto.setReporterId(bugReport.getReporter().getId());
        dto.setReporterName(bugReport.getReporter().getUsername());
        // Non-admins must not see commission breakdown.
        // - COMPANY sees the gross bounty (company is debited this amount)
        // - USER (researcher) sees the net amount (95% credit)
        // - ADMIN defaults to gross bounty (commission details are available only via admin endpoints)
        if ("COMPANY".equals(viewerRole)) {
            dto.setRewardAmount(bugReport.getBountyAmountUSD() != null ? bugReport.getBountyAmountUSD() : bugReport.getRewardAmount());
        } else {
            dto.setRewardAmount(bugReport.getResearcherNetAmountUSD() != null ? bugReport.getResearcherNetAmountUSD() : bugReport.getRewardAmount());
        }
        return dto;
    }

    private BugReportDTO convertToDTO(BugReport bugReport) {
        // Backwards compatibility: default to net.
        return convertToDTOForViewer(bugReport, "USER");
    }

    public PaginatedResult<BugReportDTO> searchBugReportsForAdmin(
            String q,
            String status,
            String severity,
            int page,
            int pageSize
    ) {
        String safeQ = q == null ? "" : q.trim().toLowerCase();
        String safeStatus = status == null ? null : status.trim().toUpperCase();
        String safeSeverity = severity == null ? null : severity.trim().toUpperCase();

        int safePage = Math.max(0, page);
        int safePageSize = Math.min(100, Math.max(1, pageSize));

        List<BugReport> all = bugReportRepository.findAll();

        List<BugReport> filtered = all.stream()
                .filter(br -> {
                    if (safeStatus != null && !safeStatus.equals(br.getStatus())) return false;
                    if (safeSeverity != null && !safeSeverity.equals(br.getSeverity())) return false;
                    if (safeQ.isEmpty()) return true;
                    return (br.getTitle() != null && br.getTitle().toLowerCase().contains(safeQ)) ||
                            (br.getDescription() != null && br.getDescription().toLowerCase().contains(safeQ));
                })
                .sorted(Comparator.comparing(BugReport::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        long total = filtered.size();
        int fromIdx = safePage * safePageSize;
        int toIdx = Math.min(filtered.size(), fromIdx + safePageSize);

        if (fromIdx >= filtered.size()) {
            return new PaginatedResult<>(List.of(), total, safePage, safePageSize);
        }

        List<BugReportDTO> items = filtered.subList(fromIdx, toIdx).stream()
                .map(br -> convertToDTOForViewer(br, "ADMIN"))
                .collect(Collectors.toList());

        return new PaginatedResult<>(items, total, safePage, safePageSize);
    }

    public PaginatedResult<BugReportDTO> searchBugReportsForReporter(
            String reporterEmail,
            String q,
            String status,
            String severity,
            int page,
            int pageSize
    ) {
        User reporter = userRepository.findByEmail(reporterEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String safeQ = q == null ? "" : q.trim().toLowerCase();
        String safeStatus = status == null ? null : status.trim().toUpperCase();
        String safeSeverity = severity == null ? null : severity.trim().toUpperCase();

        int safePage = Math.max(0, page);
        int safePageSize = Math.min(100, Math.max(1, pageSize));

        List<BugReport> all = bugReportRepository.findByReporter(reporter);

        List<BugReport> filtered = all.stream()
                .filter(br -> {
                    if (safeStatus != null && !safeStatus.equals(br.getStatus())) return false;
                    if (safeSeverity != null && !safeSeverity.equals(br.getSeverity())) return false;
                    if (safeQ.isEmpty()) return true;
                    return (br.getTitle() != null && br.getTitle().toLowerCase().contains(safeQ)) ||
                            (br.getDescription() != null && br.getDescription().toLowerCase().contains(safeQ));
                })
                .sorted(Comparator.comparing(BugReport::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        long total = filtered.size();
        int fromIdx = safePage * safePageSize;
        int toIdx = Math.min(filtered.size(), fromIdx + safePageSize);

        if (fromIdx >= filtered.size()) {
            return new PaginatedResult<>(List.of(), total, safePage, safePageSize);
        }

        List<BugReportDTO> items = filtered.subList(fromIdx, toIdx).stream()
                .map(br -> convertToDTOForViewer(br, "USER"))
                .collect(Collectors.toList());

        return new PaginatedResult<>(items, total, safePage, safePageSize);
    }

    public static class PaginatedResult<T> {
        private List<T> items;
        private long total;
        private int page;
        private int pageSize;

        public PaginatedResult(List<T> items, long total, int page, int pageSize) {
            this.items = items;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
        }

        public List<T> getItems() {
            return items;
        }

        public long getTotal() {
            return total;
        }

        public int getPage() {
            return page;
        }

        public int getPageSize() {
            return pageSize;
        }

        public int getTotalPages() {
            if (pageSize == 0) return 0;
            return (int) Math.ceil((double) total / (double) pageSize);
        }
    }
}

