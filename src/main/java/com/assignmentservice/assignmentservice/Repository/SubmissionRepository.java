package com.assignmentservice.assignmentservice.Repository;

import com.assignmentservice.assignmentservice.Model.Submission;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubmissionRepository extends MongoRepository<Submission, String> {
    void deleteByAssignmentId(String assignmentId);
}
