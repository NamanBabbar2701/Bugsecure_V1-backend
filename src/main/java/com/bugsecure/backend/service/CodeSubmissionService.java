package com.bugsecure.backend.service;

import com.bugsecure.backend.dto.CodeSubmissionDTO;
import com.bugsecure.backend.model.CodeSubmission;
import com.bugsecure.backend.model.SubmissionFile;
import com.bugsecure.backend.model.User;
import com.bugsecure.backend.repository.CodeSubmissionRepository;
import com.bugsecure.backend.repository.SubmissionFileRepository;
import com.bugsecure.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CodeSubmissionService {

    @Autowired
    private CodeSubmissionRepository codeSubmissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubmissionFileRepository submissionFileRepository;

    @Autowired
    private SecureLocalFileStorageService storageService;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional
    public CodeSubmissionDTO createSubmission(CodeSubmissionDTO dto, String email) {
        User company = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"COMPANY".equals(company.getRole())) {
            throw new RuntimeException("Only companies can create submissions");
        }

        CodeSubmission submission = new CodeSubmission();
        submission.setTitle(dto.getTitle());
        submission.setDescription(dto.getDescription());
        
        // Handle primary file (for backward compatibility)
        String primaryFileName = "code.txt";
        String primaryCodeContent = "";
        
        if (dto.getFiles() != null && !dto.getFiles().isEmpty()) {
            // Use first file as primary
            Map<String, Object> firstFile = dto.getFiles().get(0);
            primaryFileName = (String) firstFile.get("name");
            primaryCodeContent = (String) firstFile.get("content");
        } else if (dto.getFileName() != null && !dto.getFileName().isEmpty()) {
            primaryFileName = dto.getFileName();
            primaryCodeContent = dto.getCodeContent() != null ? dto.getCodeContent() : "";
        } else if (dto.getCodeContent() != null && !dto.getCodeContent().isEmpty()) {
            primaryFileName = "code.txt";
            primaryCodeContent = dto.getCodeContent();
        }
        
        submission.setFileName(primaryFileName);

        // Store primary code securely on disk (avoid storing raw code content in DB for new submissions).
        String submissionStorageFolder = "code_submissions/" + java.util.UUID.randomUUID().toString().replace("-", "");
        byte[] codeBytes = SecureLocalFileStorageService.decodeContentToBytes(primaryCodeContent);
        String codeMimeType = "text/plain";
        // If primaryCodeContent is a data URL, extract mime type.
        String extractedMime = SecureLocalFileStorageService.extractMimeTypeFromDataUrl(primaryCodeContent);
        if (extractedMime != null && !extractedMime.isBlank()) {
            codeMimeType = extractedMime;
        }
        SecureLocalFileStorageService.StoredFile storedCode = storageService.storeBytes(
                submissionStorageFolder,
                primaryFileName,
                codeMimeType,
                codeBytes
        );
        submission.setCodeStorageKey(storedCode.getStorageKey());
        submission.setCodeMimeType(storedCode.getMimeType());
        submission.setCodeSizeBytes(storedCode.getSizeBytes());
        submission.setCodeContent(null);
        
        // Reward compatibility:
        // - backend payout uses `rewardAmount` as CRITICAL base (multiplier 1.0).
        // - the UI sends a full reward breakdown; we map critical -> rewardAmount.
        Double criticalReward = dto.getRewardCriticalSeverity() != null
                ? dto.getRewardCriticalSeverity()
                : dto.getRewardAmount();
        if (criticalReward == null || criticalReward <= 0) {
            throw new RuntimeException("Critical reward amount must be greater than 0");
        }

        submission.setRewardCriticalSeverity(dto.getRewardCriticalSeverity());
        submission.setRewardLowSeverity(dto.getRewardLowSeverity());
        submission.setRewardMediumSeverity(dto.getRewardMediumSeverity());
        submission.setRewardHighSeverity(dto.getRewardHighSeverity());

        submission.setRewardAmount(criticalReward);

        // Program metadata (optional fields; backward compatible)
        submission.setWebsite(dto.getWebsite());
        submission.setInScopeTargets(dto.getInScopeTargets());
        submission.setOutOfScopeTargets(dto.getOutOfScopeTargets());
        submission.setAllowedTestingTypes(dto.getAllowedTestingTypes());
        submission.setRestrictedActions(dto.getRestrictedActions());
        submission.setEnvironmentSetting(dto.getEnvironmentSetting());
        submission.setAccessControl(dto.getAccessControl());
        submission.setTestingCredentialsEmail(dto.getTestingEmail());
        submission.setAgreedDisclosure(dto.getAgreedDisclosure());
        submission.setAgreedNoHarm(dto.getAgreedNoHarm());

        if (dto.getStartDate() != null && !dto.getStartDate().isBlank()) {
            LocalDate d = LocalDate.parse(dto.getStartDate().trim());
            submission.setProgramStartAt(d.atStartOfDay());
        }
        if (dto.getEndDate() != null && !dto.getEndDate().isBlank()) {
            LocalDate d = LocalDate.parse(dto.getEndDate().trim());
            submission.setProgramEndAt(d.atStartOfDay());
        }
        submission.setCompany(company);
        submission.setStatus("OPEN");
        submission.setCreatedAtIfNew(); // Set timestamps for MongoDB

        CodeSubmission saved = codeSubmissionRepository.save(submission);
        
        // Save additional files if provided
        if (dto.getFiles() != null && !dto.getFiles().isEmpty()) {
            for (Map<String, Object> fileData : dto.getFiles()) {
                SubmissionFile file = new SubmissionFile();
                file.setFileName((String) fileData.get("name"));
                file.setFileType((String) fileData.get("type"));
                file.setMimeType((String) fileData.get("mimeType"));
                String mimeType = (String) fileData.get("mimeType");
                String content = (String) fileData.get("content");
                byte[] bytes = SecureLocalFileStorageService.decodeContentToBytes(content);

                // If mimeType is missing but content is a data URL, try extracting it.
                if (mimeType == null || mimeType.isBlank()) {
                    String extracted = SecureLocalFileStorageService.extractMimeTypeFromDataUrl(content);
                    if (extracted != null) {
                        mimeType = extracted;
                    }
                }

                SecureLocalFileStorageService.StoredFile storedFile = storageService.storeBytes(
                        submissionStorageFolder,
                        (String) fileData.get("name"),
                        mimeType,
                        bytes
                );

                file.setStorageKey(storedFile.getStorageKey());
                file.setStorageMimeType(storedFile.getMimeType());
                file.setFileContent(null);
                file.setFileSize(((Number) fileData.get("size")).longValue());
                file.setSubmission(saved);
                file.setUploadedAtIfNew(); // Set timestamp for MongoDB
                submissionFileRepository.save(file);
            }
        }
        
        return convertToDTO(saved);
    }

    public List<CodeSubmissionDTO> getAllSubmissions() {
        return codeSubmissionRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CodeSubmissionDTO> getAllSubmissionsSorted(String sortBy) {
        List<CodeSubmission> submissions;
        
        if ("amount".equalsIgnoreCase(sortBy) || "reward".equalsIgnoreCase(sortBy)) {
            // Sort by reward amount (descending - highest first)
            submissions = codeSubmissionRepository.findAll().stream()
                    .sorted((a, b) -> {
                        Double amountA = a.getRewardAmount() != null ? a.getRewardAmount() : 0.0;
                        Double amountB = b.getRewardAmount() != null ? b.getRewardAmount() : 0.0;
                        return amountB.compareTo(amountA); // Descending
                    })
                    .collect(Collectors.toList());
        } else if ("date".equalsIgnoreCase(sortBy) || "latest".equalsIgnoreCase(sortBy)) {
            // Sort by creation date (descending - latest first)
            submissions = codeSubmissionRepository.findAll().stream()
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt()); // Descending
                    })
                    .collect(Collectors.toList());
        } else {
            // Default: no sorting
            submissions = codeSubmissionRepository.findAll();
        }
        
        return submissions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CodeSubmissionDTO> getSubmissionsByCompany(String email) {
        User company = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return codeSubmissionRepository.findByCompany(company).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CodeSubmissionDTO> getOpenSubmissions() {
        return codeSubmissionRepository.findByStatus("OPEN").stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CodeSubmissionDTO> getOpenSubmissionsSorted(String sortBy) {
        List<CodeSubmission> submissions = codeSubmissionRepository.findByStatus("OPEN");
        
        if ("amount".equalsIgnoreCase(sortBy) || "reward".equalsIgnoreCase(sortBy)) {
            // Sort by reward amount (descending - highest first)
            submissions = submissions.stream()
                    .sorted((a, b) -> {
                        Double amountA = a.getRewardAmount() != null ? a.getRewardAmount() : 0.0;
                        Double amountB = b.getRewardAmount() != null ? b.getRewardAmount() : 0.0;
                        return amountB.compareTo(amountA); // Descending
                    })
                    .collect(Collectors.toList());
        } else if ("date".equalsIgnoreCase(sortBy) || "latest".equalsIgnoreCase(sortBy)) {
            // Sort by creation date (descending - latest first)
            submissions = submissions.stream()
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt()); // Descending
                    })
                    .collect(Collectors.toList());
        }
        
        return submissions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CodeSubmissionDTO updateSubmissionStatusOnly(String id, String status, String email) {
        User company = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CodeSubmission submission = codeSubmissionRepository.findByIdAndCompany(id, company)
                .orElseThrow(() -> new RuntimeException("Submission not found or unauthorized"));

        submission.setStatus(status);
        submission.updateTimestamp(); // Update timestamp for MongoDB
        CodeSubmission updated = codeSubmissionRepository.save(submission);
        return convertToDTO(updated);
    }

    public CodeSubmissionDTO getSubmissionById(String id) {
        CodeSubmission submission = codeSubmissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Submission not found"));
        return convertToDTO(submission);
    }

    public CodeSubmissionDTO updateSubmission(String id, CodeSubmissionDTO dto, String email) {
        User company = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CodeSubmission submission = codeSubmissionRepository.findByIdAndCompany(id, company)
                .orElseThrow(() -> new RuntimeException("Submission not found or unauthorized"));

        submission.setTitle(dto.getTitle());
        submission.setDescription(dto.getDescription());
        submission.setFileName(dto.getFileName());

        // Update secure storage for code (avoid persisting raw content).
        String updatedCodeContent = dto.getCodeContent() != null ? dto.getCodeContent() : "";
        String submissionStorageFolder = "code_submissions_update/" + java.util.UUID.randomUUID().toString().replace("-", "");
        byte[] codeBytes = SecureLocalFileStorageService.decodeContentToBytes(updatedCodeContent);

        String codeMimeType = "text/plain";
        String extractedMime = SecureLocalFileStorageService.extractMimeTypeFromDataUrl(updatedCodeContent);
        if (extractedMime != null && !extractedMime.isBlank()) {
            codeMimeType = extractedMime;
        }

        SecureLocalFileStorageService.StoredFile storedCode = storageService.storeBytes(
                submissionStorageFolder,
                dto.getFileName() != null ? dto.getFileName() : "code.txt",
                codeMimeType,
                codeBytes
        );
        submission.setCodeStorageKey(storedCode.getStorageKey());
        submission.setCodeMimeType(storedCode.getMimeType());
        submission.setCodeSizeBytes(storedCode.getSizeBytes());
        submission.setCodeContent(null);

        Double criticalReward = dto.getRewardCriticalSeverity() != null
                ? dto.getRewardCriticalSeverity()
                : dto.getRewardAmount();
        if (criticalReward != null) {
            submission.setRewardCriticalSeverity(dto.getRewardCriticalSeverity());
            submission.setRewardLowSeverity(dto.getRewardLowSeverity());
            submission.setRewardMediumSeverity(dto.getRewardMediumSeverity());
            submission.setRewardHighSeverity(dto.getRewardHighSeverity());
            submission.setRewardAmount(criticalReward);
        }

        // Optional new metadata (safe no-ops if missing)
        submission.setInScopeTargets(dto.getInScopeTargets());
        submission.setOutOfScopeTargets(dto.getOutOfScopeTargets());
        submission.setAllowedTestingTypes(dto.getAllowedTestingTypes());
        submission.setRestrictedActions(dto.getRestrictedActions());
        submission.setEnvironmentSetting(dto.getEnvironmentSetting());
        submission.setAccessControl(dto.getAccessControl());
        submission.setTestingCredentialsEmail(dto.getTestingEmail());
        submission.setAgreedDisclosure(dto.getAgreedDisclosure());
        submission.setAgreedNoHarm(dto.getAgreedNoHarm());
        submission.setStatus(dto.getStatus());
        submission.updateTimestamp(); // Update timestamp for MongoDB

        CodeSubmission updated = codeSubmissionRepository.save(submission);
        return convertToDTO(updated);
    }

    public void deleteSubmission(String id, String email) {
        User company = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CodeSubmission submission = codeSubmissionRepository.findByIdAndCompany(id, company)
                .orElseThrow(() -> new RuntimeException("Submission not found or unauthorized"));

        codeSubmissionRepository.delete(submission);
    }

    private CodeSubmissionDTO convertToDTO(CodeSubmission submission) {
        CodeSubmissionDTO dto = new CodeSubmissionDTO();
        dto.setId(submission.getId());
        dto.setTitle(submission.getTitle());
        dto.setDescription(submission.getDescription());
        dto.setFileName(submission.getFileName());
        // Never return raw code content in list/detail APIs.
        dto.setCodeContent(null);
        dto.setStatus(submission.getStatus());
        dto.setRewardAmount(submission.getRewardAmount());
        dto.setWebsite(submission.getWebsite());
        dto.setCreatedAt(submission.getCreatedAt().format(formatter));
        dto.setCompanyName(submission.getCompany().getCompanyName() != null ? 
                          submission.getCompany().getCompanyName() : submission.getCompany().getUsername());
        dto.setCompanyId(submission.getCompany().getId());
        
        // Load associated files
        List<SubmissionFile> files = submissionFileRepository.findBySubmission(submission);
        List<Map<String, Object>> fileList = new ArrayList<>();
        for (SubmissionFile file : files) {
            Map<String, Object> fileMap = new HashMap<>();
            fileMap.put("id", file.getId());
            fileMap.put("name", file.getFileName());
            fileMap.put("type", file.getFileType());
            fileMap.put("mimeType", file.getMimeType());
            fileMap.put("size", file.getFileSize());
            fileList.add(fileMap);
        }
        dto.setFiles(fileList);

        // Program configuration fields (if present)
        dto.setInScopeTargets(submission.getInScopeTargets());
        dto.setOutOfScopeTargets(submission.getOutOfScopeTargets());
        dto.setAllowedTestingTypes(submission.getAllowedTestingTypes());
        dto.setRestrictedActions(submission.getRestrictedActions());
        dto.setRewardLowSeverity(submission.getRewardLowSeverity());
        dto.setRewardMediumSeverity(submission.getRewardMediumSeverity());
        dto.setRewardHighSeverity(submission.getRewardHighSeverity());
        dto.setRewardCriticalSeverity(submission.getRewardCriticalSeverity());
        dto.setEnvironmentSetting(submission.getEnvironmentSetting());
        dto.setAccessControl(submission.getAccessControl());
        dto.setTestingEmail(submission.getTestingCredentialsEmail());
        dto.setAgreedDisclosure(submission.getAgreedDisclosure());
        dto.setAgreedNoHarm(submission.getAgreedNoHarm());

        if (submission.getProgramStartAt() != null) {
            dto.setStartDate(submission.getProgramStartAt().toLocalDate().toString());
        }
        if (submission.getProgramEndAt() != null) {
            dto.setEndDate(submission.getProgramEndAt().toLocalDate().toString());
        }

        return dto;
    }
}

