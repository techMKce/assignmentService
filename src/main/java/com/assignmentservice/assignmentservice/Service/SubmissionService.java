package com.assignmentservice.assignmentservice.Service;

import com.assignmentservice.assignmentservice.Model.Submission;
import com.assignmentservice.assignmentservice.Repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    public Submission saveSubmission(Submission submission) {
        if (submission.getSubmittedAt() == null) {
            submission.setSubmittedAt(LocalDateTime.now());
        }
        return submissionRepository.save(submission);
    }

    public void deleteSubmissionByAssignmentId(String assignmentId) {
        submissionRepository.deleteByAssignmentId(assignmentId);
    }

    public List<Submission> getAllSubmission() {
        return submissionRepository.findAll();
    }
}
