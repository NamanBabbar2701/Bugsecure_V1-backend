package com.bugsecure.backend.service;

import com.bugsecure.backend.model.BugReport;
import com.bugsecure.backend.model.BugReportAttachment;
import com.bugsecure.backend.model.CodeSubmission;
import com.bugsecure.backend.model.Payment;
import com.bugsecure.backend.model.SubmissionFile;
import com.bugsecure.backend.model.TestExecution;
import com.bugsecure.backend.model.User;
import com.bugsecure.backend.model.WalletTransaction;
import com.bugsecure.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class AccountDeletionService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BugReportRepository bugReportRepository;

    @Autowired
    private CodeSubmissionRepository codeSubmissionRepository;

    @Autowired
    private SubmissionFileRepository submissionFileRepository;

    @Autowired
    private BugReportAttachmentRepository bugReportAttachmentRepository;

    @Autowired
    private TestExecutionRepository testExecutionRepository;

    /**
     * Permanent deletion with cascade cleanup of Mongo documents.
     *
     * Note: uploaded file bytes are kept on disk (storageKey documents are deleted, but files remain).
     */
    @Transactional
    public void permanentlyDeleteUser(String userId) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete payments first (they reference bug reports and users).
        List<Payment> paymentsToDelete = new ArrayList<>();
        paymentsToDelete.addAll(paymentRepository.findByCompany(target));
        paymentsToDelete.addAll(paymentRepository.findByResearcher(target));
        if (!paymentsToDelete.isEmpty()) {
            paymentRepository.deleteAll(paymentsToDelete);
        }

        // Delete bug reports + attachments.
        if ("USER".equals(target.getRole())) {
            deleteBugReportsAndAttachmentsByReporter(target);
        } else if ("COMPANY".equals(target.getRole())) {
            // A company account can also act as a reporter. Clean both sides:
            // 1) bug reports authored by the company
            // 2) bug reports linked to the company's code submissions
            deleteBugReportsAndAttachmentsByReporter(target);
            deleteCodeSubmissionsAndAllDependentsForCompany(target);
        }

        // Delete test executions created by this researcher.
        List<TestExecution> executionsToDelete = testExecutionRepository.findByResearcherOrderByCreatedAtDesc(target);
        if (!executionsToDelete.isEmpty()) {
            testExecutionRepository.deleteAll(executionsToDelete);
        }

        // Delete wallet transactions owned by this user.
        List<WalletTransaction> walletTx = walletTransactionRepository.findByUser(target);
        if (!walletTx.isEmpty()) {
            walletTransactionRepository.deleteAll(walletTx);
        }

        userRepository.delete(target);
    }

    private void deleteBugReportsAndAttachmentsByReporter(User reporter) {
        List<BugReport> reports = bugReportRepository.findByReporter(reporter);
        if (reports.isEmpty()) return;

        for (BugReport report : reports) {
            List<BugReportAttachment> attachments = bugReportAttachmentRepository.findByBugReport(report);
            if (!attachments.isEmpty()) {
                bugReportAttachmentRepository.deleteAll(attachments);
            }
        }
        bugReportRepository.deleteAll(reports);
    }

    private void deleteCodeSubmissionsAndAllDependentsForCompany(User company) {
        List<CodeSubmission> submissions = codeSubmissionRepository.findByCompany(company);
        if (submissions.isEmpty()) return;

        for (CodeSubmission submission : submissions) {
            // Delete test executions that target this submission (researchers only).
            List<TestExecution> executions =
                    testExecutionRepository.findBySubmissionIdOrderByCreatedAtDesc(submission.getId());
            if (!executions.isEmpty()) {
                testExecutionRepository.deleteAll(executions);
            }

            // Delete submission files metadata.
            List<SubmissionFile> files = submissionFileRepository.findBySubmission(submission);
            if (!files.isEmpty()) {
                submissionFileRepository.deleteAll(files);
            }

            // Delete bug reports + their attachments metadata.
            List<BugReport> reports = bugReportRepository.findByCodeSubmission(submission);
            if (!reports.isEmpty()) {
                for (BugReport report : reports) {
                    List<BugReportAttachment> attachments = bugReportAttachmentRepository.findByBugReport(report);
                    if (!attachments.isEmpty()) {
                        bugReportAttachmentRepository.deleteAll(attachments);
                    }
                }
                bugReportRepository.deleteAll(reports);
            }

            // Delete the submission metadata itself.
            codeSubmissionRepository.delete(submission);
        }
    }
}

