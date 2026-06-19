package com.bugsecure.backend.repository;

import com.bugsecure.backend.model.HttpTestCase;
import com.bugsecure.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HttpTestCaseRepository extends MongoRepository<HttpTestCase, String> {
    List<HttpTestCase> findByResearcherOrderByCreatedAtDesc(User researcher);
}

