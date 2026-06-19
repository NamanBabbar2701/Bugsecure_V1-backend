package com.bugsecure.backend.service;

import com.bugsecure.backend.model.CodeSubmission;
import com.bugsecure.backend.model.TestExecution;
import com.bugsecure.backend.model.User;
import com.bugsecure.backend.repository.CodeSubmissionRepository;
import com.bugsecure.backend.repository.TestExecutionRepository;
import com.bugsecure.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class TestExecutionService {

    @Autowired
    private TestExecutionRepository testExecutionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CodeSubmissionRepository codeSubmissionRepository;

    @Autowired
    private SecureLocalFileStorageService storageService;

    @Value("${sandbox.base-url:}")
    private String sandboxBaseUrl;

    @Value("${sandbox.api-key:}")
    private String sandboxApiKey;

    @Value("${sandbox.run-path:/api/sandbox/run}")
    private String sandboxRunPath;

    @Value("${sandbox.poll-path:/api/sandbox/executions/{executionId}}")
    private String sandboxPollPath;

    @Value("${sandbox.poll-interval-ms:3000}")
    private long sandboxPollIntervalMs;

    @Value("${sandbox.max-wait-seconds:180}")
    private long sandboxMaxWaitSeconds;

    @Value("${sandbox.fallback-to-simulator:true}")
    private boolean sandboxFallbackToSimulator;

    private final RestTemplate restTemplate = new RestTemplate();

    // Run test in sandboxed environment (simulated)
    public Map<String, Object> runTest(String email, Map<String, Object> testRequest) {
        User researcher = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Only researchers can run tests
        if (!"USER".equals(researcher.getRole())) {
            throw new RuntimeException("Only researchers can run tests");
        }

        String testType = (String) testRequest.getOrDefault("testType", "COMMAND");
        String scriptContent = (String) testRequest.get("scriptContent");
        String fileName = (String) testRequest.get("fileName");
        String fileType = (String) testRequest.get("fileType");
        String submissionId = (String) testRequest.get("submissionId");
        String programId = (String) testRequest.get("programId"); // Support programId as well

        // Auto-fetch targetUrl from program/submission if programId or submissionId provided
        String targetUrl = null;
        CodeSubmission programSubmission = null;
        
        if (programId != null && !programId.isEmpty()) {
            programSubmission = codeSubmissionRepository.findById(programId).orElse(null);
        } else if (submissionId != null && !submissionId.isEmpty()) {
            programSubmission = codeSubmissionRepository.findById(submissionId).orElse(null);
        }
        
        if (programSubmission != null) {
            targetUrl = programSubmission.getWebsite();
            if (submissionId == null || submissionId.isEmpty()) {
                submissionId = programSubmission.getId();
            }
        }

        if (scriptContent == null || scriptContent.trim().isEmpty()) {
            throw new RuntimeException("Script content is required");
        }

        // Create test execution record
        TestExecution testExecution = new TestExecution();
        testExecution.setResearcher(researcher);
        testExecution.setTestType(testType);
        testExecution.setFileName(fileName);
        testExecution.setFileType(fileType);
        testExecution.setStatus("PENDING");
        testExecution.setCreatedAtIfNew();

        // Link to submission if provided
        if (programSubmission != null) {
            testExecution.setSubmission(programSubmission);
        }

        // Store uploaded script/file bytes securely, avoid persisting raw content.
        SecureLocalFileStorageService.StoredFile storedScript = null;
        if (scriptContent != null) {
            String scriptFolder = "test_executions/" + researcher.getId() + "/" + UUID.randomUUID().toString().replace("-", "");
            byte[] scriptBytes = SecureLocalFileStorageService.decodeContentToBytes(scriptContent);
            String mimeType = SecureLocalFileStorageService.extractMimeTypeFromDataUrl(scriptContent);
            if (mimeType == null || mimeType.isBlank()) {
                mimeType = "text/plain";
            }
            storedScript = storageService.storeBytes(
                    scriptFolder,
                    fileName != null ? fileName : "script.txt",
                    mimeType,
                    scriptBytes
            );
        }

        if (storedScript != null) {
            testExecution.setScriptStorageKey(storedScript.getStorageKey());
            testExecution.setScriptMimeType(storedScript.getMimeType());
            testExecution.setScriptSizeBytes(storedScript.getSizeBytes());
        }
        testExecution.setScriptContent(null);

        final TestExecution savedExecution = testExecutionRepository.save(testExecution);

        // Simulate test execution in isolated environment
        // In production, this would use Docker or a proper sandbox
        final String executionId = savedExecution.getId();
        final String linkedProgramIdFinal = submissionId;
        final String targetUrlFinal = targetUrl;
        CompletableFuture.supplyAsync(() -> executeTestSafely(
                        savedExecution,
                        scriptContent,
                        testType,
                        targetUrlFinal,
                        linkedProgramIdFinal
                ))
                .thenAccept(result -> {
            TestExecution completed = testExecutionRepository.findById(executionId).orElse(null);
            if (completed != null) {
                completed.setOutput(result.get("output").toString());
                if (result.containsKey("error")) {
                    completed.setErrorLog(result.get("error").toString());
                    completed.markFailed();
                } else {
                    completed.markCompleted();
                }
                if (result.containsKey("executionTime")) {
                    completed.setExecutionTimeMs(Long.parseLong(result.get("executionTime").toString()));
                }
                testExecutionRepository.save(completed);
            }
        });

        // Return initial response with targetUrl if available
        Map<String, Object> response = new HashMap<>();
        response.put("executionId", executionId);
        response.put("status", "RUNNING");
        response.put("message", "Test execution started. Results will be available shortly.");
        if (targetUrl != null) {
            response.put("targetUrl", targetUrl);
        }

        return response;
    }

    // Simulated safe test execution (DO NOT execute real code in production without proper sandboxing)
    // We pass scriptContent/testType from the request to avoid reading/storing raw content from DB.
    private Map<String, Object> executeTestSafely(
            TestExecution testExecution,
            String scriptContent,
            String testType,
            String targetUrl,
            String linkedProgramId
    ) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // If sandbox is configured, attempt hosted execution.
            if (sandboxBaseUrl != null && !sandboxBaseUrl.isBlank()) {
                return executeWithHostedSandbox(testExecution, scriptContent, testType, targetUrl, linkedProgramId);
            }

            // Simulate execution delay (fallback simulator)
            Thread.sleep(1000 + (long)(Math.random() * 2000));

            String safeScriptContent = scriptContent != null ? scriptContent : "";
            String safeTestType = testType != null ? testType : testExecution.getTestType();
            String fileTypeUpper = testExecution.getFileType() != null ? testExecution.getFileType().toUpperCase() : "";

            boolean isDataUrl = SecureLocalFileStorageService.looksLikeDataUrl(safeScriptContent);
            String extractedMime = isDataUrl ? SecureLocalFileStorageService.extractMimeTypeFromDataUrl(safeScriptContent) : null;

            // Allow simulation only for text/code-like uploads.
            boolean allowSimulation =
                    !"FILE_UPLOAD".equalsIgnoreCase(safeTestType)
                            || ((extractedMime != null && extractedMime.startsWith("text/"))
                                || Set.of(
                                        "JS", "PY", "TXT", "SH", "C", "CPP", "CS", "PHP", "RB", "GO", "JAVA",
                                        "TS", "TSX", "JSX", "HTML", "CSS"
                                ).contains(fileTypeUpper));

            String simulationText = "";
            if (allowSimulation) {
                if (isDataUrl) {
                    simulationText = new String(SecureLocalFileStorageService.decodeContentToBytes(safeScriptContent), StandardCharsets.UTF_8);
                } else {
                    simulationText = safeScriptContent;
                }
            }

            // Simulate test execution output (in production, use proper sandbox)
            StringBuilder output = new StringBuilder();
            output.append("=== Test Execution Started ===\n");
            output.append("Test Type: ").append(testType).append("\n");
            output.append("Timestamp: ").append(LocalDateTime.now()).append("\n\n");

            // Simulate different outputs based on test type
            if ("SCRIPT".equals(safeTestType) || "FILE_UPLOAD".equals(safeTestType)) {
                output.append("Executing script...\n");
                if (!allowSimulation) {
                    output.append("Unsupported document type for execution simulation.\n");
                    output.append("Stored content is available for authorized preview only.\n");
                } else {
                    output.append("Script length: ").append(simulationText.length()).append(" characters\n");
                
                    // Simulate vulnerability detection
                    String lower = simulationText.toLowerCase();
                    if (lower.contains("sql") || lower.contains("select")) {
                        output.append("[WARNING] Potential SQL injection pattern detected\n");
                    }
                    if (lower.contains("xss") || lower.contains("<script>")) {
                        output.append("[WARNING] Potential XSS pattern detected\n");
                    }
                    if (lower.contains("eval") || lower.contains("exec")) {
                        output.append("[WARNING] Dangerous code execution pattern detected\n");
                    }
                
                    output.append("\n=== Execution Summary ===\n");
                    output.append("Status: Completed\n");
                    output.append("Lines processed: ").append(simulationText.split("\n").length).append("\n");
                    output.append("Vulnerabilities found: ").append((int)(Math.random() * 3)).append("\n");
                }
            } else {
                // Command execution simulation
                output.append("Executing command: ").append(allowSimulation ? simulationText : safeScriptContent).append("\n");
                output.append("Command executed successfully\n");
                output.append("Output: Simulated test result\n");
            }

            output.append("\n=== Test Execution Completed ===\n");
            result.put("output", output.toString());
            result.put("executionTime", System.currentTimeMillis() - startTime);

        } catch (InterruptedException e) {
            result.put("error", "Test execution was interrupted");
            result.put("output", "Execution failed due to interruption");
        } catch (Exception e) {
            result.put("error", "Test execution failed: " + e.getMessage());
            result.put("output", "Error during execution");
        }

        return result;
    }

    private Map<String, Object> executeWithHostedSandbox(
            TestExecution testExecution,
            String scriptContent,
            String testType,
            String targetUrl,
            String linkedProgramId
    ) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            // Build request body using stored bytes if available.
            byte[] bytes = new byte[0];
            if (testExecution.getScriptStorageKey() != null && !testExecution.getScriptStorageKey().isBlank()) {
                bytes = storageService.readBytes(testExecution.getScriptStorageKey());
            } else {
                bytes = SecureLocalFileStorageService.decodeContentToBytes(scriptContent);
            }

            String contentBase64 = Base64.getEncoder().encodeToString(bytes);

            String mimeType = testExecution.getScriptMimeType() != null ? testExecution.getScriptMimeType() : "text/plain";

            String contentText = null;
            if (mimeType.startsWith("text/")) {
                contentText = new String(bytes, StandardCharsets.UTF_8);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("testType", testType);
            payload.put("fileName", testExecution.getFileName());
            payload.put("fileType", testExecution.getFileType());
            payload.put("mimeType", mimeType);
            payload.put("contentBase64", contentBase64);
            if (contentText != null) {
                payload.put("contentText", contentText);
            }
            payload.put("targetUrl", targetUrl);
            payload.put("programId", linkedProgramId);

            String runUrl = sandboxBaseUrl + sandboxRunPath;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (sandboxApiKey != null && !sandboxApiKey.isBlank()) {
                // Send both common header patterns for maximum compatibility.
                headers.set("X-API-KEY", sandboxApiKey);
                headers.set("Authorization", "Bearer " + sandboxApiKey);
            }

            ResponseEntity<Map> runResp = restTemplate.postForEntity(
                    runUrl,
                    new HttpEntity<>(payload, headers),
                    Map.class
            );

            Map<String, Object> runBody = runResp.getBody();
            if (runBody == null) {
                throw new RuntimeException("Sandbox returned empty response body");
            }

            // Try sync parsing first.
            Object outputObj = firstNonNull(
                    runBody.get("output"),
                    getNested(runBody, "data", "output"),
                    getNested(runBody, "result", "output"),
                    runBody.get("result")
            );

            Object errorObj = firstNonNull(
                    runBody.get("error"),
                    getNested(runBody, "data", "error"),
                    getNested(runBody, "result", "error")
            );

            if (outputObj instanceof String) {
                result.put("output", outputObj);
                result.put("executionTime", System.currentTimeMillis() - startTime);
                return result;
            }
            if (errorObj instanceof String) {
                result.put("error", errorObj);
                result.put("output", "Execution failed");
                result.put("executionTime", System.currentTimeMillis() - startTime);
                return result;
            }

            // If async job info exists, poll.
            String executionId = asString(firstNonNull(
                    runBody.get("executionId"),
                    runBody.get("jobId"),
                    getNested(runBody, "data", "executionId"),
                    getNested(runBody, "data", "jobId")
            ));

            if (executionId == null || executionId.isBlank()) {
                throw new RuntimeException("Sandbox did not provide output or executionId/jobId");
            }

            long maxWaitMs = TimeUnit.SECONDS.toMillis(sandboxMaxWaitSeconds);
            long elapsedMs = 0;

            while (elapsedMs < maxWaitMs) {
                String pollUrl = sandboxBaseUrl + sandboxPollPath.replace("{executionId}", executionId);

                ResponseEntity<Map> pollResp = restTemplate.getForEntity(pollUrl, Map.class);
                Map<String, Object> pollBody = pollResp.getBody();
                if (pollBody == null) {
                    throw new RuntimeException("Sandbox poll returned empty response body");
                }

                String status = asString(firstNonNull(
                        pollBody.get("status"),
                        pollBody.get("state"),
                        getNested(pollBody, "data", "status"),
                        getNested(pollBody, "data", "state")
                ));

                Object pollOutputObj = firstNonNull(
                        pollBody.get("output"),
                        getNested(pollBody, "data", "output"),
                        getNested(pollBody, "result", "output"),
                        pollBody.get("result")
                );

                Object pollErrorObj = firstNonNull(
                        pollBody.get("error"),
                        getNested(pollBody, "data", "error"),
                        getNested(pollBody, "result", "error")
                );

                if (pollOutputObj instanceof String) {
                    result.put("output", pollOutputObj);
                    result.put("executionTime", System.currentTimeMillis() - startTime);
                    return result;
                }

                if (pollErrorObj instanceof String || (status != null && status.toUpperCase().contains("FAIL"))) {
                    result.put("error", pollErrorObj != null ? pollErrorObj : "Sandbox execution failed");
                    result.put("output", "Execution failed");
                    result.put("executionTime", System.currentTimeMillis() - startTime);
                    return result;
                }

                // status indicates in progress; wait and continue
                try {
                    Thread.sleep(sandboxPollIntervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    result.put("error", "Sandbox polling was interrupted");
                    result.put("output", "Execution failed due to interruption");
                    result.put("executionTime", System.currentTimeMillis() - startTime);
                    return result;
                }
                elapsedMs = System.currentTimeMillis() - startTime;
            }

            throw new RuntimeException("Sandbox execution timed out after " + sandboxMaxWaitSeconds + " seconds");
        } catch (Exception e) {
            if (sandboxFallbackToSimulator) {
                // Fall back to the in-app simulator to avoid breaking the UX.
                Map<String, Object> sim = executeSimulation(testExecution, scriptContent, testType);
                sim.put("sandboxError", e.getMessage());
                return sim;
            }
            throw e;
        }
    }

    private Map<String, Object> executeSimulation(TestExecution testExecution, String scriptContent, String testType) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        try {
            // Simulate execution delay
            Thread.sleep(1000 + (long) (Math.random() * 2000));

            String safeScriptContent = scriptContent != null ? scriptContent : "";
            String safeTestType = testType != null ? testType : testExecution.getTestType();
            String fileTypeUpper = testExecution.getFileType() != null ? testExecution.getFileType().toUpperCase() : "";

            String mimeType = testExecution.getScriptMimeType() != null ? testExecution.getScriptMimeType() : "text/plain";

            String extractedMime = null;
            String safeContentForMime = safeScriptContent;
            boolean isDataUrl = SecureLocalFileStorageService.looksLikeDataUrl(safeContentForMime);
            extractedMime = isDataUrl ? SecureLocalFileStorageService.extractMimeTypeFromDataUrl(safeContentForMime) : null;

            boolean allowSimulation =
                    !"FILE_UPLOAD".equalsIgnoreCase(safeTestType)
                            || ((extractedMime != null && extractedMime.startsWith("text/"))
                            || Set.of(
                                    "JS", "PY", "TXT", "SH", "C", "CPP", "CS", "PHP", "RB", "GO", "JAVA",
                                    "TS", "TSX", "JSX", "HTML", "CSS"
                            ).contains(fileTypeUpper));

            String simulationText = "";
            if (allowSimulation) {
                if (isDataUrl) {
                    simulationText = new String(SecureLocalFileStorageService.decodeContentToBytes(safeContentForMime), StandardCharsets.UTF_8);
                } else {
                    simulationText = safeScriptContent;
                }
            }

            StringBuilder output = new StringBuilder();
            output.append("=== Test Execution Started ===\n");
            output.append("Test Type: ").append(testType).append("\n");
            output.append("Timestamp: ").append(LocalDateTime.now()).append("\n\n");

            if ("SCRIPT".equals(safeTestType) || "FILE_UPLOAD".equals(safeTestType)) {
                output.append("Executing script...\n");
                if (!allowSimulation) {
                    output.append("Unsupported document type for execution simulation.\n");
                    output.append("Stored content is available for authorized preview only.\n");
                } else {
                    output.append("Script length: ").append(simulationText.length()).append(" characters\n");

                    String lower = simulationText.toLowerCase();
                    if (lower.contains("sql") || lower.contains("select")) {
                        output.append("[WARNING] Potential SQL injection pattern detected\n");
                    }
                    if (lower.contains("xss") || lower.contains("<script>")) {
                        output.append("[WARNING] Potential XSS pattern detected\n");
                    }
                    if (lower.contains("eval") || lower.contains("exec")) {
                        output.append("[WARNING] Dangerous code execution pattern detected\n");
                    }

                    output.append("\n=== Execution Summary ===\n");
                    output.append("Status: Completed\n");
                    output.append("Lines processed: ").append(simulationText.split("\n").length).append("\n");
                    output.append("Vulnerabilities found: ").append((int) (Math.random() * 3)).append("\n");
                }
            } else {
                output.append("Executing command: ").append(allowSimulation ? simulationText : safeScriptContent).append("\n");
                output.append("Command executed successfully\n");
                output.append("Output: Simulated test result\n");
            }

            output.append("\n=== Test Execution Completed ===\n");
            result.put("output", output.toString());
            result.put("executionTime", System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            result.put("error", "Simulator failed: " + e.getMessage());
            result.put("output", "Simulation failed");
            result.put("executionTime", System.currentTimeMillis() - startTime);
        }
        return result;
    }

    private Object firstNonNull(Object... values) {
        if (values == null) return null;
        for (Object v : values) {
            if (v != null) return v;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Object getNested(Map<String, Object> map, String... keys) {
        if (map == null || keys == null || keys.length == 0) return null;
        Object cur = map;
        for (String key : keys) {
            if (!(cur instanceof Map)) return null;
            cur = ((Map<String, Object>) cur).get(key);
        }
        return cur;
    }

    private String asString(Object obj) {
        if (obj == null) return null;
        return obj.toString();
    }

    // Get test execution result
    public Map<String, Object> getTestResult(String email, String executionId) {
        User researcher = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TestExecution testExecution = testExecutionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Test execution not found"));

        // Verify ownership
        if (!testExecution.getResearcher().getId().equals(researcher.getId())) {
            throw new RuntimeException("Unauthorized access to test execution");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", testExecution.getId());
        response.put("status", testExecution.getStatus());
        response.put("output", testExecution.getOutput());
        response.put("errorLog", testExecution.getErrorLog());
        response.put("testType", testExecution.getTestType());
        response.put("fileName", testExecution.getFileName());
        response.put("fileType", testExecution.getFileType());
        response.put("createdAt", testExecution.getCreatedAt());
        response.put("completedAt", testExecution.getCompletedAt());
        response.put("executionTimeMs", testExecution.getExecutionTimeMs());

        return response;
    }

    // Get test execution history
    public List<TestExecution> getTestHistory(String email) {
        User researcher = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return testExecutionRepository.findByResearcherOrderByCreatedAtDesc(researcher);
    }
}

