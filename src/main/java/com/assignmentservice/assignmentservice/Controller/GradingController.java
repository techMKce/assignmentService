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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@CrossOrigin(origins = "http://localhost:8080")
@RequestMapping("/api/v1/gradings")
public class GradingController {

    private static final Logger logger = LoggerFactory.getLogger(GradingController.class);

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
                        return ResponseEntity.status(500)
                                .body(new ErrorResponse("Error assigning grade: " + e.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.badRequest()
                        .body(new ErrorResponse("Student Roll Number and Assignment ID cannot be null or empty")));
    }

    @GetMapping
    public ResponseEntity<?> getSubmissionsAndGradingsByAssignmentId(
            @RequestParam("assignmentId") String assignmentId) {
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
                                .body(new ErrorResponse(
                                        "Error retrieving submissions and gradings: " + e.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.badRequest()
                        .body(new ErrorResponse("Assignment ID cannot be null or blank")));
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadGrades(@RequestParam("assignmentId") String assignmentId) {
        return Optional.ofNullable(assignmentId)
                .filter(s -> !s.isBlank())
                .map(id -> {
                    try {
                        // Fetch Course Name and Assignment Title for filename
                        Assignment assignment = assignmentService.getAssignmentById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Assignment not found for ID: " + id));
                        String courseName = assignment.getCourseName();
                        String assignmentTitle = assignment.getTitle();

                        // Sanitize for filename
                        String sanitizedCourseName = courseName.replaceAll("[^a-zA-Z0-9]", "_");
                        String sanitizedAssignmentTitle = assignmentTitle.replaceAll("[^a-zA-Z0-9]", "_");
                        String filename = String.format("%s-%s.csv", sanitizedCourseName, sanitizedAssignmentTitle);

                        String csvContent = gradingService.generateGradingCsvForAssignment(id);

                        return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                                .contentType(MediaType.parseMediaType("text/csv"))
                                .body(csvContent);
                    } catch (IllegalArgumentException e) {
                        logger.error("Error generating grading CSV: {}", e.getMessage());
                        return ResponseEntity.badRequest()
                                .body(Map.of("message", "Error: " + e.getMessage()));
                    } catch (IOException e) {
                        logger.error("IO error generating grading CSV: {}", e.getMessage());
                        return ResponseEntity.status(500)
                                .body(Map.of("message", "Error generating CSV: " + e.getMessage()));
                    }
                })
                .orElseGet(() -> {
                    logger.error("Assignment ID is null or blank");
                    return ResponseEntity.badRequest()
                            .body(Map.of("message", "Assignment ID cannot be null or blank"));
                });
    }
    
    // New endpoint to fetch a specific grading by studentRollNumber and assignmentId
    @GetMapping("/grade")
    public ResponseEntity<?> getGradingByStudentAndAssignment(
            @RequestParam("studentRollNumber") String studentRollNumber,
            @RequestParam("assignmentId") String assignmentId) {
        return Optional.ofNullable(studentRollNumber)
                .filter(s -> !s.isBlank())
                .flatMap(roll -> Optional.ofNullable(assignmentId)
                        .filter(a -> !a.isBlank())
                        .map(a -> {
                            try {
                                Optional<Grading> grading = gradingService.getGradingByStudentRollNumberAndAssignmentId(
                                        studentRollNumber, assignmentId);
                                if (grading.isPresent()) {
                                    return ResponseEntity.ok(Map.of(
                                            "message", "Grading retrieved successfully",
                                            "grading", grading.get()));
                                } else {
                                    return ResponseEntity.ok(Map.of(
                                            "message", "No grading found",
                                            "grading", null));
                                }
                            } catch (Exception e) {
                                return ResponseEntity.status(500)
                                        .body(new ErrorResponse("Error retrieving grading: " + e.getMessage()));
                            }
                        }))
                .orElseGet(() -> ResponseEntity.badRequest()
                        .body(new ErrorResponse("Student Roll Number and Assignment ID cannot be null or blank")));
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
                        return ResponseEntity.status(500)
                                .body(new ErrorResponse("Error deleting grade: " + e.getMessage()));
                    }
                })
                .orElseGet(() -> ResponseEntity.badRequest()
                        .body(new ErrorResponse("Student Roll Number and Assignment ID cannot be empty")));
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