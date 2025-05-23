package com.assignmentservice.assignmentservice.Controller;

import com.assignmentservice.assignmentservice.Model.Grading;
import com.assignmentservice.assignmentservice.Model.Submission;
import com.assignmentservice.assignmentservice.Service.GradingService;
import com.assignmentservice.assignmentservice.Service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/gradings")
public class GradingController {

    @Autowired
    private GradingService gradingService;

    @Autowired
    private SubmissionService submissionService;

    @PostMapping("/assign")
    public ResponseEntity<?> assignGrade(@RequestBody GradeAssignmentRequest request) {
        try {
            Grading grading = gradingService.assignGrade(
                    request.getUserId(),
                    request.getAssignmentId(),
                    request.getGrade(),
                    request.getFeedback());
            return ResponseEntity.ok(grading);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Error assigning grade: " + e.getMessage()));
        }
    }

    @PostMapping("/assignment")
    public ResponseEntity<?> getSubmissionsAndGradingsByAssignmentId(@RequestBody AssignmentIdRequest request) {
        String assignmentId = request.getAssignmentId();
        if (assignmentId == null || assignmentId.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Assignment ID cannot be null or blank"));
        }

        try {
            List<Submission> submissions = submissionService.getSubmissionsByAssignmentId(assignmentId);
            List<Grading> gradings = gradingService.getGradingsByAssignmentId(assignmentId);
            return ResponseEntity.ok(new SubmissionGradingResponse(submissions, gradings));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Error retrieving submissions and gradings: " + e.getMessage()));
        }
    }

    @GetMapping("/download")
    public ResponseEntity<String> downloadGrades() {
        try {
            String csvContent = gradingService.generateGradesCsv();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=student_grades.csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csvContent);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error generating CSV: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteAssignedGrade(@RequestBody GradeAssignmentRequest request) {
        try {
            Grading updatedGrading = gradingService.deleteAssignedGrade(
                    request.getUserId(),
                    request.getAssignmentId()
            );
            return ResponseEntity.ok(updatedGrading);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Error deleting grade: " + e.getMessage()));
        }
    }

    public static class GradeAssignmentRequest {
        private String userId;
        private String assignmentId;
        private String grade;
        private String feedback;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getAssignmentId() {
            return assignmentId;
        }

        public void setAssignmentId(String assignmentId) {
            this.assignmentId = assignmentId;
        }

        public String getGrade() {
            return grade;
        }

        public void setGrade(String grade) {
            this.grade = grade;
        }

        public String getFeedback() {
            return feedback;
        }

        public void setFeedback(String feedback) {
            this.feedback = feedback;
        }
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

    public static class SubmissionGradingResponse {
        private List<Submission> submissions;
        private List<Grading> gradings;

        public SubmissionGradingResponse(List<Submission> submissions, List<Grading> gradings) {
            this.submissions = submissions;
            this.gradings = gradings;
        }

        public List<Submission> getSubmissions() {
            return submissions;
        }

        public void setSubmissions(List<Submission> submissions) {
            this.submissions = submissions;
        }

        public List<Grading> getGradings() {
            return gradings;
        }

        public void setGradings(List<Grading> gradings) {
            this.gradings = gradings;
        }
    }

    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
