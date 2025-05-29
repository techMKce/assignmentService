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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "http://localhost:8080")
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
        return Optional.ofNullable(request)
                .filter(req -> Optional.ofNullable(req.getStudentRollNumber()).filter(s -> !s.isBlank()).isPresent())
                .filter(req -> Optional.ofNullable(req.getAssignmentId()).filter(s -> !s.isBlank()).isPresent())
                .map(req -> {
                    try {
                        Grading grading = gradingService.assignGrade(
                                req.getStudentRollNumber(),
                                req.getAssignmentId(),
                                req.getGrade(),
                                req.getFeedback());
                        return ResponseEntity.ok(Map.of(
                                "message", "Grade assigned successfully",
                                "grading", grading));
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
                    } catch (Exception e) {
                        return ResponseEntity.status(500).body(new ErrorResponse("Error assigning grade: " + e.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.badRequest().body(new ErrorResponse("Student Roll Number and Assignment ID cannot be null or empty")));
    }

    @GetMapping
    public ResponseEntity<?> getSubmissionsAndGradingsByAssignmentId(@RequestParam("assignmentId") String assignmentId) {
        return Optional.ofNullable(assignmentId)
                .filter(s -> !s.isBlank())
                .map(id -> {
                    try {
                        List<Submission> submissions = submissionService.getSubmissionsByAssignmentId(id);
                        List<Grading> gradings = gradingService.getGradingsByAssignmentId(id);
                        return ResponseEntity.ok(Map.of(
                                "message", "Submissions and gradings retrieved successfully",
                                "submissions", submissions,
                                "gradings", gradings));
                    } catch (Exception e) {
                        return ResponseEntity.status(500)
                                .body(new ErrorResponse("Error retrieving submissions and gradings: " + e.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.badRequest().body(new ErrorResponse("Assignment ID cannot be null or blank")));
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadGrades(@RequestParam("assignmentId") String assignmentId) {
        return Optional.ofNullable(assignmentId)
                .filter(s -> !s.isBlank())
                .map(id -> {
                    try {
                        Assignment assignment = assignmentService.getAssignmentById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Assignment not found for ID: " + id));
                        String assignmentName = assignment.getTitle().replaceAll("[^a-zA-Z0-9]", "_");
                        String courseId = Optional.ofNullable(assignment.getCourseId())
                                .filter(s -> !s.isBlank())
                                .orElse("Unknown");
                        String filename = String.format("%s_%s.csv", assignmentName, courseId);

                        String csvContent = gradingService.generateGradesCsvForAssignment(id);

                        return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                                .contentType(MediaType.parseMediaType("text/csv"))
                                .body(csvContent);
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(new ErrorResponse("Error: " + e.getMessage()));
                    } catch (IOException e) {
                        return ResponseEntity.status(500).body(new ErrorResponse("Error generating CSV: " + e.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.badRequest().body(new ErrorResponse("Assignment ID cannot be null or blank")));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAssignedGrade(@RequestBody GradeAssignmentRequest request) {
        return Optional.ofNullable(request)
                .filter(req -> Optional.ofNullable(req.getStudentRollNumber()).filter(s -> !s.isBlank()).isPresent())
                .filter(req -> Optional.ofNullable(req.getAssignmentId()).filter(s -> !s.isBlank()).isPresent())
                .map(req -> {
                    try {
                        Grading updatedGrading = gradingService.deleteAssignedGrade(
                                req.getStudentRollNumber(),
                                req.getAssignmentId());
                        return ResponseEntity.ok(Map.of(
                                "message", "Grade deleted successfully",
                                "grading", updatedGrading));
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
                    } catch (Exception e) {
                        return ResponseEntity.status(500).body(new ErrorResponse("Error deleting grade: " + e.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.badRequest().body(new ErrorResponse("Student Roll Number and Assignment ID cannot be empty")));
    }

    public static class GradeAssignmentRequest {
        private String studentRollNumber;
        private String assignmentId;
        private String grade;
        private String feedback;

        public String getStudentRollNumber() { return studentRollNumber; }
        public void setStudentRollNumber(String studentRollNumber) { this.studentRollNumber = studentRollNumber; }
        public String getAssignmentId() { return assignmentId; }
        public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; }
        public String getGrade() { return grade; }
        public void setGrade(String grade) { this.grade = grade; }
        public String getFeedback() { return feedback; }
        public void setFeedback(String feedback) { this.feedback = feedback; }
    }

    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}