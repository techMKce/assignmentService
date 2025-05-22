package com.assignmentservice.assignmentservice.Controller;

import com.assignmentservice.assignmentservice.Model.Submission;
import com.assignmentservice.assignmentservice.Service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/assignmentsubmissions")
@CrossOrigin(origins = "http://localhost:3000")
public class SubmissionController {

    @Autowired
    private SubmissionService submissionService;

    @PostMapping("/submitassignment")
    public ResponseEntity<Submission> createSubmission(@Valid @RequestBody Submission submission) {
        Submission savedSubmission = submissionService.saveSubmission(submission);
        return ResponseEntity.ok(savedSubmission);
    }

    @GetMapping
    public ResponseEntity<List<Submission>> getAllSubmission() {
        List<Submission> submissions = submissionService.getAllSubmission();
        return ResponseEntity.ok(submissions);
    }

    @DeleteMapping("/deleteassignment")
    public ResponseEntity<Void> deleteSubmission(@Valid @RequestBody AssignmentIdRequest idRequest) {
        String assignmentId = idRequest.getAssignmentId();
        if (assignmentId == null || assignmentId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        submissionService.deleteSubmissionByAssignmentId(assignmentId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    public static class AssignmentIdRequest {
        private String assignmentId;

        public String getAssignmentId() {
            return assignmentId;
        }

        public void setAssignmentId(String assignmentId) {
            this.assignmentId = assignmentId;
        }
    }
}
