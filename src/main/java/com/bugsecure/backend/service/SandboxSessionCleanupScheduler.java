package com.bugsecure.backend.service;

import com.bugsecure.backend.model.SandboxSession;
import com.bugsecure.backend.repository.SandboxSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SandboxSessionCleanupScheduler {

    @Autowired
    private SandboxSessionRepository sandboxSessionRepository;

    @Autowired
    private SandboxService sandboxService;

    @Scheduled(fixedDelayString = "${sandbox.cleanup.interval-ms:60000}")
    public void cleanupExpiredSandboxes() {
        LocalDateTime now = LocalDateTime.now();
        try {
            List<SandboxSession> expired = sandboxSessionRepository.findByStatusAndExpiresAtBefore(
                    SandboxSession.STATUS_RUNNING,
                    now
            );
            for (SandboxSession session : expired) {
                try {
                    sandboxService.stopSandboxInternal(session, "ttl expired");
                } catch (Exception ignored) {
                    // Cleanup should be resilient.
                }
            }
        } catch (Exception ignored) {
            // Avoid scheduler crashes.
        }
    }
}

