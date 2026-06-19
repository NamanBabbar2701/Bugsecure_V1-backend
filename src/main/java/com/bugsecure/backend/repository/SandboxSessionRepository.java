package com.bugsecure.backend.repository;

import com.bugsecure.backend.model.SandboxSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SandboxSessionRepository extends MongoRepository<SandboxSession, String> {

    List<SandboxSession> findByStatusAndExpiresAtBefore(String status, LocalDateTime before);

    long countByResearcherUserIdAndStatus(String researcherUserId, String status);

    List<SandboxSession> findByResearcherUserIdAndStatusOrderByCreatedAtDesc(String researcherUserId, String status);
}

