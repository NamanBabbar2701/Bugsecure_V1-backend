package com.bugsecure.backend.repository;

import com.bugsecure.backend.model.Payment;
import com.bugsecure.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
    List<Payment> findByCompany(User company);
    List<Payment> findByResearcher(User researcher);
    List<Payment> findByStatus(String status);
    Optional<Payment> findByBugReportId(String bugReportId);
    List<Payment> findByCompanyAndStatus(User company, String status);
    List<Payment> findByResearcherAndStatus(User researcher, String status);
}







