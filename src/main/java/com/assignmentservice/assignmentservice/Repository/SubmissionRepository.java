package com.assignmentservice.assignmentservice.Repository;

import com.assignmentservice.assignmentservice.Model.Submission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends MongoRepository<Submission, String> {
    void deleteByAssignmentId(String assignmentId);

    List<Submission> findByAssignmentId(String assignmentId);

    @Query("{ 'assignmentId': ?0, 'userId': ?1 }")
    Optional<Submission> findByAssignmentIdAndUserId(String assignmentId, String userId);

    @Query(value = "{ 'assignmentId': ?0, 'userId': ?1 }", delete = true)
    void deleteByAssignmentIdAndUserId(String assignmentId, String userId);
}