package com.assignmentservice.assignmentservice.Repository;

import com.assignmentservice.assignmentservice.Model.Submission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends MongoRepository<Submission, String> {
    void deleteByAssignmentId(String assignmentId);

    List<Submission> findByAssignmentId(String assignmentId);

    List<Submission> findByAssignmentIdAndUserId(String assignmentId, String userId);

    void deleteByAssignmentIdAndUserId(String assignmentId, String userId);
}
