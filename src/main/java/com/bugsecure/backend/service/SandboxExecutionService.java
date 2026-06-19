package com.bugsecure.backend.service;

import com.bugsecure.backend.model.SandboxExecution;
import com.bugsecure.backend.model.SandboxSession;
import com.bugsecure.backend.model.User;
import com.bugsecure.backend.repository.SandboxExecutionRepository;
import com.bugsecure.backend.repository.SandboxSessionRepository;
import com.bugsecure.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class SandboxExecutionService {

    @Autowired
    private SandboxSessionRepository sandboxSessionRepository;

    @Autowired
    private SandboxExecutionRepository sandboxExecutionRepository;

    @Autowired
    private DockerCliExecutor dockerCli;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${sandbox.exec.work-dir:./sandbox_exec_workdir}")
    private String execWorkDir;

    @Value("${sandbox.exec.timeout-seconds:20}")
    private int defaultTimeoutSeconds;

    public SandboxExecution executeRunCode(String sandboxId, String requesterEmail, SandboxExecutionRequest request) {
        SandboxSession session = authorizeAndGetSession(sandboxId, requesterEmail);

        SandboxExecution execution = new SandboxExecution();
        execution.setSandboxSessionId(session.getId());
        execution.setSubmissionId(session.getSubmissionId());
        execution.setResearcherUserId(session.getResearcherUserId());
        execution.setTaskType("RUN_CODE");
        execution.setCreatedAt(LocalDateTime.now());
        execution.setTimeoutSeconds(request.timeoutSeconds != null ? request.timeoutSeconds : defaultTimeoutSeconds);

        execution.setLanguage(request.language);
        execution.setMainClass(request.mainClass != null ? request.mainClass : "Main");
        execution.setSourceFileName(request.sourceFileName);

        execution = sandboxExecutionRepository.save(execution);

        int timeoutSeconds = execution.getTimeoutSeconds() != null ? execution.getTimeoutSeconds() : defaultTimeoutSeconds;

        Path hostExecDir = null;
        try {
            hostExecDir = prepareHostDir(execution.getId());
            String fileName = determineSourceFileName(execution.getLanguage(), request.sourceFileName);
            Path hostSourceFile = hostExecDir.resolve(fileName);
            Files.writeString(hostSourceFile, request.sourceCode != null ? request.sourceCode : "", StandardCharsets.UTF_8);

            // Copy source into container workdir
            String containerName = session.getDockerContainerName();
            String containerDir = "/sandbox/work/" + execution.getId();
            dockerCli.execute(List.of("docker", "exec", containerName, "sh", "-c", "mkdir -p " + containerDir), Duration.ofSeconds(10));

            String hostPathForDocker = hostSourceFile.toAbsolutePath().toString().replace("\\", "/");
            dockerCli.execute(
                    List.of("docker", "cp", hostPathForDocker, containerName + ":" + containerDir + "/"),
                    Duration.ofSeconds(30)
            );

            // Execute compile/run inside container.
            DockerRunResult run = dockerRunCodeInContainer(containerName, containerDir, execution, fileName, timeoutSeconds);

            execution.setStdout(run.stdout);
            execution.setStderr(run.stderr);
            execution.setStatus(run.success ? SandboxExecution.STATUS_COMPLETED : SandboxExecution.STATUS_FAILED);
            execution.setFailureReason(run.success ? null : run.failureReason);
            execution.setCompletedAt(LocalDateTime.now());
            execution.setDurationMs(run.durationMs);
            execution.setResultJson(run.resultJson);

            return sandboxExecutionRepository.save(execution);
        } catch (Exception e) {
            execution.setStatus(SandboxExecution.STATUS_FAILED);
            execution.setFailureReason(e.getMessage());
            execution.setCompletedAt(LocalDateTime.now());
            execution.setDurationMs(0L);
            return sandboxExecutionRepository.save(execution);
        } finally {
            if (hostExecDir != null) {
                deleteQuietly(hostExecDir);
            }
        }
    }

    public SandboxExecution executeSecurityScan(String sandboxId, String requesterEmail, SandboxExecutionSecurityScanRequest request) {
        SandboxSession session = authorizeAndGetSession(sandboxId, requesterEmail);

        SandboxExecution execution = new SandboxExecution();
        execution.setSandboxSessionId(session.getId());
        execution.setSubmissionId(session.getSubmissionId());
        execution.setResearcherUserId(session.getResearcherUserId());
        execution.setTaskType("SECURITY_SCAN");
        execution.setCreatedAt(LocalDateTime.now());
        execution.setTimeoutSeconds(request.timeoutSeconds != null ? request.timeoutSeconds : defaultTimeoutSeconds);
        execution = sandboxExecutionRepository.save(execution);

        int timeoutSeconds = execution.getTimeoutSeconds() != null ? execution.getTimeoutSeconds() : defaultTimeoutSeconds;

        try {
            String containerName = session.getDockerContainerName();
            String scanCmd = "timeout --signal=KILL " + timeoutSeconds + "s python3 /sandbox/runner/security_scan.py /sandbox/www";

            DockerRunResult run = dockerCliExec(containerName, scanCmd, timeoutSeconds);

            execution.setStdout(run.stdout);
            execution.setStderr(run.stderr);
            execution.setStatus(run.success ? SandboxExecution.STATUS_COMPLETED : SandboxExecution.STATUS_FAILED);
            execution.setFailureReason(run.success ? null : run.failureReason);
            execution.setCompletedAt(LocalDateTime.now());
            execution.setDurationMs(run.durationMs);
            execution.setResultJson(run.resultJson);

            return sandboxExecutionRepository.save(execution);
        } catch (Exception e) {
            execution.setStatus(SandboxExecution.STATUS_FAILED);
            execution.setFailureReason(e.getMessage());
            execution.setCompletedAt(LocalDateTime.now());
            execution.setDurationMs(0L);
            return sandboxExecutionRepository.save(execution);
        }
    }

    private SandboxSession authorizeAndGetSession(String sandboxId, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SandboxSession session = sandboxSessionRepository.findById(sandboxId)
                .orElseThrow(() -> new RuntimeException("Sandbox session not found"));

        boolean isAdmin = "ADMIN".equals(requester.getRole());
        boolean isOwner = requester.getId() != null && requester.getId().equals(session.getResearcherUserId());
        if (!isAdmin && !isOwner) {
            throw new RuntimeException("Unauthorized to execute inside this sandbox");
        }

        return session;
    }

    private Path prepareHostDir(String executionId) throws IOException {
        Path root = Path.of(execWorkDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
        Path dir = root.resolve("exec_" + executionId).normalize();
        Files.createDirectories(dir);
        return dir;
    }

    private String determineSourceFileName(String language, String sourceFileName) {
        String lang = language != null ? language.toLowerCase(Locale.ROOT) : "";
        if (sourceFileName != null && !sourceFileName.isBlank()) return sanitizeFileName(sourceFileName);

        if ("python".equals(lang)) return "main.py";
        if ("node".equals(lang) || "javascript".equals(lang)) return "main.js";
        if ("java".equals(lang)) return "Main.java";
        throw new RuntimeException("Unsupported language: " + language);
    }

    private DockerRunResult dockerRunCodeInContainer(
            String containerName,
            String containerDir,
            SandboxExecution execution,
            String fileName,
            int timeoutSeconds
    ) throws Exception {
        String cmd;
        String lang = execution.getLanguage();

        if ("python".equalsIgnoreCase(lang)) {
            cmd = "timeout --signal=KILL " + timeoutSeconds + "s python3 " + containerDir + "/" + fileName;
        } else if ("node".equalsIgnoreCase(lang) || "javascript".equalsIgnoreCase(lang) || "js".equalsIgnoreCase(lang)) {
            cmd = "timeout --signal=KILL " + timeoutSeconds + "s node " + containerDir + "/" + fileName;
        } else if ("java".equalsIgnoreCase(lang)) {
            // Compile then run. We assume mainClass is Main by default.
            String mainClass = execution.getMainClass() != null ? execution.getMainClass() : "Main";
            cmd = "cd " + containerDir + " && timeout --signal=KILL " + timeoutSeconds + "s javac " + fileName +
                    " && timeout --signal=KILL " + timeoutSeconds + "s java -cp " + containerDir + " " + mainClass;
        } else {
            throw new RuntimeException("Unsupported language: " + lang);
        }

        return dockerCliExec(containerName, cmd, timeoutSeconds);
    }

    private DockerRunResult dockerCliExec(String containerName, String cmd, int timeoutSeconds) throws Exception {
        long start = System.currentTimeMillis();
        try {
            String execCmd = cmd;
            DockerCliExecutor.Result r = dockerCli.execute(
                    List.of("docker", "exec", containerName, "sh", "-c", execCmd),
                    Duration.ofSeconds(timeoutSeconds + 10L)
            );

            String stdout = r.getStdout() != null ? r.getStdout() : "";
            String stderr = r.getStderr() != null ? r.getStderr() : "";

            boolean success = r.getExitCode() == 0;
            String failureReason = success ? null : ("Non-zero exit code: " + r.getExitCode());

            String resultJson = tryParseAsJson(stdout);

            return new DockerRunResult(success, stdout, stderr, failureReason, resultJson, System.currentTimeMillis() - start);
        } catch (RuntimeException e) {
            return new DockerRunResult(false, "", e.getMessage(), e.getMessage(), null, System.currentTimeMillis() - start);
        }
    }

    private String tryParseAsJson(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            JsonNode node = objectMapper.readTree(text);
            // Store minified JSON for storage.
            return node.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void deleteQuietly(Path dir) {
        try {
            if (dir == null) return;
            if (!Files.exists(dir)) return;
            Files.walk(dir)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
    }

    private String sanitizeFileName(String name) {
        String n = name.replace("\\", "/");
        if (n.contains("/")) n = n.substring(n.lastIndexOf('/') + 1);
        n = n.replaceAll("[^a-zA-Z0-9._-]", "_");
        return n == null || n.isBlank() ? "main.txt" : n;
    }

    private static class DockerRunResult {
        final boolean success;
        final String stdout;
        final String stderr;
        final String failureReason;
        final String resultJson;
        final long durationMs;

        private DockerRunResult(boolean success, String stdout, String stderr, String failureReason, String resultJson, long durationMs) {
            this.success = success;
            this.stdout = stdout;
            this.stderr = stderr;
            this.failureReason = failureReason;
            this.resultJson = resultJson;
            this.durationMs = durationMs;
        }
    }

    // Request DTOs (kept as simple POJOs).
    public static class SandboxExecutionRequest {
        public String taskType; // unused, but kept for extensibility
        public String language; // python|node|java
        public String sourceCode; // the code to execute
        public String sourceFileName; // optional
        public String mainClass; // for java
        public Integer timeoutSeconds;
    }

    public static class SandboxExecutionSecurityScanRequest {
        public Integer timeoutSeconds;
    }
}

