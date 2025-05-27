package com.assignmentservice.assignmentservice.Repository;

import com.assignmentservice.assignmentservice.Model.Grading;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradingRepository extends MongoRepository<Grading, String> {
    Optional<Grading> findByStudentRollNumberAndAssignmentId(String studentRollNumber, String assignmentId);
    List<Grading> findByAssignmentId(String assignmentId);
    void deleteByStudentRollNumberAndAssignmentId(String studentRollNumber, String assignmentId);
}