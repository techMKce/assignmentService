package com.assignmentservice.assignmentservice.Repository;

import com.assignmentservice.assignmentservice.Model.Grading;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradingRepository extends MongoRepository<Grading, String> {
    @Query("{ 'studentRollNumber': ?0, 'assignmentId': ?1 }")
    Optional<Grading> findByStudentRollNumberAndAssignmentId(String studentRollNumber, String assignmentId);
    
    @Query("{ 'assignmentId': ?0 }")
    List<Grading> findByAssignmentId(String assignmentId);

     Optional<Grading> getGradingByStudentRollNumberAndAssignmentId(String studentRollNumber, String assignmentId);

    void deleteByStudentRollNumberAndAssignmentId(String studentRollNumber, String assignmentId);
}