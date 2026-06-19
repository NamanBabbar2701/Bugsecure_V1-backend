package com.bugsecure.backend.repository;

import com.bugsecure.backend.model.HttpRequestLog;
import com.bugsecure.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HttpRequestLogRepository extends MongoRepository<HttpRequestLog, String> {
    List<HttpRequestLog> findByResearcherOrderByCreatedAtDesc(User researcher);
}

