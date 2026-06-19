package com.bugsecure.backend.repository;

import com.bugsecure.backend.model.CodeSubmission;
import com.bugsecure.backend.model.SubmissionFile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionFileRepository extends MongoRepository<SubmissionFile, String> {
    List<SubmissionFile> findBySubmission(CodeSubmission submission);
    List<SubmissionFile> findBySubmissionId(String submissionId);
}







