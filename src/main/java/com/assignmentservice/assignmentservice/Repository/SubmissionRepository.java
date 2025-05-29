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

    @Query("{ 'assignmentId': ?0, 'studentRollNumber': ?1 }")
    Optional<Submission> findByAssignmentIdAndStudentRollNumber(String assignmentId, String studentRollNumber);

    @Query(value = "{ 'assignmentId': ?0, 'studentRollNumber': ?1 }", delete = true)
    void deleteByAssignmentIdAndStudentRollNumber(String assignmentId, String studentRollNumber);

    @Query(value = "{ 'studentRollNumber': ?0, 'assignmentId': { $in: ?1 }, 'status': 'Accepted' }", count = true)
    long countByStudentRollNumberAndAssignmentIdInAndStatusAccepted(String studentRollNumber, List<String> assignmentIds);

    @Query(value = "{'assignmentId': {$in: ?0}}", fields = "{'studentRollNumber': 1}")
    List<Submission> findDistinctStudentRollNumbersByAssignmentIds(List<String> assignmentIds);

    @Query("{ 'studentRollNumber': ?0, 'assignmentId': { $in: ?1 } }")
    List<Submission> findByStudentRollNumberAndAssignmentIdIn(String studentRollNumber, List<String> assignmentIds);
}