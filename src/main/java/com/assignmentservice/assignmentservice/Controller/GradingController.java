package com.assignmentservice.assignmentservice.Controller;

import com.assignmentservice.assignmentservice.Model.Assignment;
import com.assignmentservice.assignmentservice.Model.Grading;
import com.assignmentservice.assignmentservice.Model.Submission;
import com.assignmentservice.assignmentservice.Service.AssignmentService;
import com.assignmentservice.assignmentservice.Service.GradingService;
import com.assignmentservice.assignmentservice.Service.SubmissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:8085")
@RequestMapping("/api/gradings")
public class GradingController {

    @Autowired
    private GradingService gradingService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private AssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<?> assignGrade(@RequestBody GradeAssignmentRequest request) {
        try {
            Grading grading = gradingService.assignGrade(
                    request.getStudentRollNumber(),
                    request.getAssignmentId(),
                    request.getGrade(),
                    request.getFeedback());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Grade assigned successfully");
            response.put("grading", grading);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Error assigning grade: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getSubmissionsAndGradingsByAssignmentId(@RequestParam("assignmentId") String assignmentId) {
        if (assignmentId == null || assignmentId.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Assignment ID cannot be null or blank"));
        }

        try {
            List<Submission> submissions = submissionService.getSubmissionsByAssignmentId(assignmentId);
            List<Grading> gradings = gradingService.getGradingsByAssignmentId(assignmentId);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Submissions and gradings retrieved successfully");
            response.put("submissions", submissions);
            response.put("gradings", gradings);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Error retrieving submissions and gradings: " + e.getMessage()));
        }
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadGrades(@RequestParam("assignmentId") String assignmentId) {
        if (assignmentId == null || assignmentId.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Assignment ID cannot be null or blank"));
        }

        try {
            Assignment assignment = assignmentService.getAssignmentById(assignmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Assignment not found for ID: " + assignmentId));
            String assignmentName = assignment.getTitle().replaceAll("[^a-zA-Z0-9]", "_");

            String csvContent = gradingService.generateGradesCsvForAssignment(assignmentId);

            String filename = assignmentName + ".csv";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csvContent);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Error: " + e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Error generating CSV: " + e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAssignedGrade(@RequestBody GradeAssignmentRequest request) {
        try {
            Grading updatedGrading = gradingService.deleteAssignedGrade(
                    request.getStudentRollNumber(),
                    request.getAssignmentId());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Grade deleted successfully");
            response.put("grading", updatedGrading);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Error deleting grade: " + e.getMessage()));
        }
    }

    public static class GradeAssignmentRequest {
        private String studentRollNumber;
        private String assignmentId;
        private String grade;
        private String feedback;

        public String getStudentRollNumber() {
            return studentRollNumber;
        }

        public void setStudentRollNumber(String studentRollNumber) {
            this.studentRollNumber = studentRollNumber;
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