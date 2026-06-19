package com.bugsecure.backend.service;

import com.bugsecure.backend.model.CodeSubmission;
import com.bugsecure.backend.model.SandboxSession;
import com.bugsecure.backend.model.SubmissionFile;
import com.bugsecure.backend.model.User;
import com.bugsecure.backend.repository.CodeSubmissionRepository;
import com.bugsecure.backend.repository.SandboxSessionRepository;
import com.bugsecure.backend.repository.SubmissionFileRepository;
import com.bugsecure.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.ServerSocket;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class SandboxService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CodeSubmissionRepository codeSubmissionRepository;

    @Autowired
    private SubmissionFileRepository submissionFileRepository;

    @Autowired
    private SandboxSessionRepository sandboxSessionRepository;

    @Autowired
    private SandboxPackagingService packagingService;

    @Autowired
    private DockerSandboxService dockerSandboxService;

    @Value("${sandbox.session.ttl-seconds:300}")
    private long ttlSeconds;

    @Value("${sandbox.max-sessions-per-user:3}")
    private int maxSessionsPerUser;

    public SandboxSession startSandbox(String submissionId, String researcherEmail) {
        User researcher = userRepository.findByEmail(researcherEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"USER".equals(researcher.getRole()) && !"ADMIN".equals(researcher.getRole())) {
            throw new RuntimeException("Only researchers can start sandbox testing");
        }

        if ("USER".equals(researcher.getRole()) && (researcher.getContractAccepted() == null || !researcher.getContractAccepted())) {
            throw new RuntimeException("Contract must be accepted before sandbox testing");
        }

        long runningSessions = sandboxSessionRepository.countByResearcherUserIdAndStatus(
                researcher.getId(),
                SandboxSession.STATUS_RUNNING
        );
        if (runningSessions >= maxSessionsPerUser) {
            throw new RuntimeException("Sandbox limit reached. Stop active sandboxes and try again.");
        }

        CodeSubmission submission = codeSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        // Create DB record first so we have a stable sandboxId for naming.
        SandboxSession session = new SandboxSession();
        session.setSubmissionId(submission.getId());
        session.setResearcherUserId(researcher.getId());
        session.setResearcherUsername(researcher.getUsername());
        session.setDockerImage(dockerSandboxService.getSandboxImage());
        session.setStatus(SandboxSession.STATUS_STARTING);
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusSeconds(ttlSeconds));

        session = sandboxSessionRepository.save(session);

        String containerName = buildContainerName(session.getId());
        int hostPort = findFreeHostPort();

        session.setDockerContainerName(containerName);
        session.setHostPort(hostPort);
        sandboxSessionRepository.save(session);

        Path packagedDir = null;
        try {
            List<SubmissionFile> files = submissionFileRepository.findBySubmissionId(submissionId);
            SandboxPackagingService.PackagedSubmission packaged = packagingService.packageSubmission(submission, files);
            packagedDir = packaged.getLocalDir();

            dockerSandboxService.createCopyAndStart(containerName, hostPort, packagedDir);

            session.setStatus(SandboxSession.STATUS_RUNNING);
            sandboxSessionRepository.save(session);
            return session;
        } catch (Exception e) {
            session.setStatus(SandboxSession.STATUS_FAILED);
            session.setFailureReason(e.getMessage());
            session.setStoppedAt(LocalDateTime.now());
            try {
                dockerSandboxService.stopQuietly(containerName);
            } catch (Exception ignored) {
            }
            sandboxSessionRepository.save(session);
            throw e;
        } finally {
            if (packagedDir != null) {
                packagingService.deleteQuietly(packagedDir);
            }
        }
    }

    public SandboxSession stopSandbox(String sandboxId, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SandboxSession session = sandboxSessionRepository.findById(sandboxId)
                .orElseThrow(() -> new RuntimeException("Sandbox session not found"));

        boolean isAdmin = "ADMIN".equals(requester.getRole());
        boolean isOwner = Objects.equals(session.getResearcherUserId(), requester.getId());
        if (!isAdmin && !isOwner) {
            throw new RuntimeException("Unauthorized to stop this sandbox session");
        }

        return stopSandboxInternal(session, "manual stop");
    }

    public SandboxSession stopSandboxInternal(SandboxSession session, String reason) {
        if (session == null) {
            throw new IllegalArgumentException("session is required");
        }

        if (SandboxSession.STATUS_STOPPED.equals(session.getStatus()) || SandboxSession.STATUS_FAILED.equals(session.getStatus())) {
            return session;
        }

        String containerName = session.getDockerContainerName();
        session.setStatus(SandboxSession.STATUS_STOPPED);
        session.setFailureReason(null);
        session.setStoppedAt(LocalDateTime.now());

        try {
            String logs = dockerSandboxService.stopAndRemove(containerName);
            session.setStopLogs(truncate(logs, 100_000));
        } catch (Exception e) {
            // Stopping must not break scheduled cleanup.
            session.setStopLogs(truncate(String.valueOf(e.getMessage()), 10_000));
            session.setFailureReason(reason + ": " + e.getMessage());
        }

        sandboxSessionRepository.save(session);
        return session;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen);
    }

    private String buildContainerName(String sandboxId) {
        // Docker names must be lowercase and limited in length.
        String suffix = sandboxId != null ? sandboxId.replaceAll("[^a-zA-Z0-9]", "").toLowerCase() : "unknown";
        if (suffix.length() > 30) suffix = suffix.substring(0, 30);
        return "bugsecure-sb-" + suffix;
    }

    private int findFreeHostPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new RuntimeException("Failed to find free host port: " + e.getMessage());
        }
    }
}

