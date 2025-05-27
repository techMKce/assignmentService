package com.assignmentservice.assignmentservice.Repository;

import com.assignmentservice.assignmentservice.Model.Grading;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface GradingRepository extends MongoRepository<Grading, String> {
    Optional<Grading> findBystudentRollNumberAndAssignmentId(String studentRollNumber, String assignmentId);
    List<Grading> findByAssignmentId(String assignmentId);
    void deleteBystudentRollNumberAndAssignmentId(String studentRollNumber, String assignmentId);
}