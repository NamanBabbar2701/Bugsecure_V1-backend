package com.bugsecure.backend.repository;

import com.bugsecure.backend.model.BugReport;
import com.bugsecure.backend.model.CodeSubmission;
import com.bugsecure.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BugReportRepository extends MongoRepository<BugReport, String> {
    List<BugReport> findByReporter(User reporter);
    List<BugReport> findByCodeSubmission(CodeSubmission codeSubmission);
    List<BugReport> findByCodeSubmissionAndStatus(CodeSubmission codeSubmission, String status);
    List<BugReport> findByStatus(String status);
    Optional<BugReport> findByIdAndReporter(String id, User reporter);
}







