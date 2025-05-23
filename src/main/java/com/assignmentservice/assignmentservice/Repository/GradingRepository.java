package com.assignmentservice.assignmentservice.Repository;

import com.assignmentservice.assignmentservice.Model.Grading;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface GradingRepository extends MongoRepository<Grading, String> {
    Optional<Grading> findByUserIdAndAssignmentId(String userId, String assignmentId);
    List<Grading> findByAssignmentId(String assignmentId);
    void deleteByUserIdAndAssignmentId(String userId, String assignmentId);
}
