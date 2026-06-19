package com.bugsecure.backend.repository;

import com.bugsecure.backend.model.TestExecution;
import com.bugsecure.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestExecutionRepository extends MongoRepository<TestExecution, String> {
    List<TestExecution> findByResearcherOrderByCreatedAtDesc(User researcher);
    List<TestExecution> findBySubmissionIdOrderByCreatedAtDesc(String submissionId);
    List<TestExecution> findByResearcherAndStatusOrderByCreatedAtDesc(User researcher, String status);
}






