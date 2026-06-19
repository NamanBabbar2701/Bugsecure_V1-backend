package com.bugsecure.backend.repository;

import com.bugsecure.backend.model.SandboxExecution;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SandboxExecutionRepository extends MongoRepository<SandboxExecution, String> {

    List<SandboxExecution> findBySandboxSessionIdOrderByCreatedAtDesc(String sandboxSessionId);
}

