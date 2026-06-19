package com.bugsecure.backend.repository;

import com.bugsecure.backend.model.CodeSubmission;
import com.bugsecure.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodeSubmissionRepository extends MongoRepository<CodeSubmission, String> {
    List<CodeSubmission> findByCompany(User company);
    List<CodeSubmission> findByStatus(String status);
    Optional<CodeSubmission> findByIdAndCompany(String id, User company);
}







