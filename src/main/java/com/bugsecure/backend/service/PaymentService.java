package com.bugsecure.backend.service;

import com.bugsecure.backend.dto.PaymentDTO;
import com.bugsecure.backend.model.BugReport;
import com.bugsecure.backend.model.Payment;
import com.bugsecure.backend.model.User;
import com.bugsecure.backend.repository.BugReportRepository;
import com.bugsecure.backend.repository.PaymentRepository;
import com.bugsecure.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BugReportRepository bugReportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletService walletService;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final double USD_TO_INR_RATE = 83.0;

    public PaymentDTO createPayment(String bugReportId, String paymentMethod, String email) {
        BugReport bugReport = bugReportRepository.findById(bugReportId)
                .orElseThrow(() -> new RuntimeException("Bug report not found"));

        if (!"APPROVED".equals(bugReport.getStatus())) {
            throw new RuntimeException("Bug report must be approved before creating payment");
        }

        User company = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!bugReport.getCodeSubmission().getCompany().getEmail().equals(email)) {
            throw new RuntimeException("Only the company that owns the submission can create payments");
        }

        // Check if payment already exists
        paymentRepository.findByBugReportId(bugReport.getId()).ifPresent(payment -> {
            throw new RuntimeException("Payment already exists for this bug report");
        });

        // BugReport.rewardAmount is treated as researcher net in this phase.
        Double bountyUSD = bugReport.getBountyAmountUSD() != null ? bugReport.getBountyAmountUSD() : bugReport.getRewardAmount();
        Double netUSD = bugReport.getResearcherNetAmountUSD() != null ? bugReport.getResearcherNetAmountUSD() : bugReport.getRewardAmount();
        Double commissionUSD = bugReport.getPlatformCommissionAmountUSD() != null ? bugReport.getPlatformCommissionAmountUSD() : 0.0;

        if (bountyUSD == null || bountyUSD <= 0) {
            throw new RuntimeException("Invalid bounty amount");
        }
        if (netUSD == null || netUSD < 0) {
            throw new RuntimeException("Invalid net reward amount");
        }

        Double netINR = netUSD * USD_TO_INR_RATE;
        Double bountyINR = bountyUSD * USD_TO_INR_RATE;
        Double commissionINR = commissionUSD * USD_TO_INR_RATE;

        Payment payment = new Payment();
        // amountUSD/INR are treated as "researcher net" in this phase.
        payment.setAmountUSD(netUSD);
        payment.setAmountINR(netINR);

        payment.setBountyAmountUSD(bountyUSD);
        payment.setBountyAmountINR(bountyINR);
        payment.setResearcherNetAmountUSD(netUSD);
        payment.setResearcherNetAmountINR(netINR);
        payment.setPlatformCommissionAmountUSD(commissionUSD);
        payment.setPlatformCommissionAmountINR(commissionINR);

        payment.setPaymentMethod(paymentMethod);
        payment.setBugReport(bugReport);
        payment.setCompany(company);
        payment.setResearcher(bugReport.getReporter());
        payment.setStatus("PENDING");
        payment.setCreatedAtIfNew(); // Set timestamps for MongoDB

        Payment saved = paymentRepository.save(payment);
        return convertToDTO(saved, company.getRole());
    }

    public List<PaymentDTO> getPaymentsByCompany(String email) {
        User company = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return paymentRepository.findByCompany(company).stream()
                .map(p -> convertToDTO(p, "COMPANY"))
                .collect(Collectors.toList());
    }

    public List<PaymentDTO> getPaymentsByResearcher(String email) {
        User researcher = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return paymentRepository.findByResearcher(researcher).stream()
                .map(p -> convertToDTO(p, "USER"))
                .collect(Collectors.toList());
    }

    public PaymentDTO updatePaymentStatus(String paymentId, String status, String transactionId, String notes, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        // Only company or admin can update payment status
        if (!"ADMIN".equals(user.getRole()) && 
            !payment.getCompany().getEmail().equals(email)) {
            throw new RuntimeException("Unauthorized to update payment status");
        }

        payment.setStatus(status);
        if (transactionId != null) {
            payment.setTransactionId(transactionId);
        }
        if (notes != null) {
            payment.setNotes(notes);
        }
        
        payment.updateTimestamp(); // Update timestamp for MongoDB

        Payment updated = paymentRepository.save(payment);

        // In this phase, wallet credit happens when the bug report is APPROVED.
        // For backwards compatibility, only credit if wallet split hasn't run.
        if ("COMPLETED".equals(status) && updated.getResearcher() != null) {
            try {
                boolean splitDone = updated.getBugReport() != null && updated.getBugReport().getWalletSplitCompleted();
                if (!splitDone) {
                    walletService.addReward(
                            updated.getResearcher().getEmail(),
                            updated.getAmountUSD(), // net
                            updated.getId(),
                            "Bug bounty reward for: " + (updated.getBugReport() != null ? updated.getBugReport().getTitle() : "unknown submission")
                    );
                }
            } catch (Exception e) {
                // Log error but don't fail the payment update
                System.err.println("Failed to add reward to wallet: " + e.getMessage());
            }
        }

        return convertToDTO(updated, user.getRole());
    }

    public PaymentDTO getPaymentById(String id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        // Default to researcher net view.
        return convertToDTO(payment, "USER");
    }

    public List<PaymentDTO> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(p -> convertToDTO(p, "USER"))
                .collect(Collectors.toList());
    }

    private PaymentDTO convertToDTO(Payment payment, String viewerRole) {
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        if ("COMPANY".equals(viewerRole)) {
            dto.setAmountUSD(payment.getBountyAmountUSD() != null ? payment.getBountyAmountUSD() : payment.getAmountUSD());
            dto.setAmountINR(payment.getBountyAmountINR() != null ? payment.getBountyAmountINR() : payment.getAmountINR());
        } else {
            dto.setAmountUSD(payment.getAmountUSD()); // net
            dto.setAmountINR(payment.getAmountINR());
        }
        dto.setStatus(payment.getStatus());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setTransactionId(payment.getTransactionId());
        dto.setNotes(payment.getNotes());
        dto.setCreatedAt(payment.getCreatedAt().format(formatter));
        dto.setBugReportId(payment.getBugReport().getId());
        dto.setBugReportTitle(payment.getBugReport().getTitle());
        dto.setCompanyId(payment.getCompany().getId());
        dto.setCompanyName(payment.getCompany().getCompanyName() != null ? 
                          payment.getCompany().getCompanyName() : payment.getCompany().getUsername());
        dto.setResearcherId(payment.getResearcher().getId());
        dto.setResearcherName(payment.getResearcher().getUsername());
        return dto;
    }
}







