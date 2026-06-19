package com.bugsecure.backend.repository;

import com.bugsecure.backend.model.BugReport;
import com.bugsecure.backend.model.BugReportAttachment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BugReportAttachmentRepository extends MongoRepository<BugReportAttachment, String> {

    List<BugReportAttachment> findByBugReport(BugReport bugReport);

    Optional<BugReportAttachment> findByIdAndBugReport(String id, BugReport bugReport);
}

