package com.assignmentservice.assignmentservice.Controller;

import com.assignmentservice.assignmentservice.Model.Assignment;
import com.assignmentservice.assignmentservice.Model.Submission;
import com.assignmentservice.assignmentservice.Service.AssignmentService;
import com.assignmentservice.assignmentservice.Service.SubmissionService;
import com.assignmentservice.assignmentservice.Service.FileService;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionController.class);

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private FileService fileService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private com.assignmentservice.assignmentservice.Repository.GradingRepository gradingRepository;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> submitAssignment(
            @RequestParam("assignmentId") String assignmentId,
            @RequestParam("studentName") String studentName,
            @RequestParam("studentRollNumber") String studentRollNumber,
            @RequestPart("file") MultipartFile file) throws IOException {
        try {
            Submission submission = submissionService.saveSubmission(assignmentId, studentName, studentRollNumber, file);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Submission created successfully");
            response.put("submissionId", submission.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to submit assignment for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId, e);
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to submit assignment: " + e.getMessage()));
        }
    }

    @PostMapping("/status")
    public ResponseEntity<?> updateSubmissionStatus(@RequestBody UpdateSubmissionStatusRequest request) {
        try {
            Assignment assignment = assignmentService.getAssignmentById(request.getAssignmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Assignment not found for ID: " + request.getAssignmentId()));
            Submission submission = submissionService.updateSubmissionStatus(
                request.getSubmissionId(), 
                request.getStatus(),
                assignment.getTitle()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Submission status updated to " + request.getStatus());
            response.put("submission", submission);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to update submission status: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error updating submission status: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getSubmissionsByAssignmentId(@RequestParam("assignmentId") String assignmentId) {
        if (assignmentId == null || assignmentId.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Assignment ID cannot be null or blank"));
        }
        List<Submission> submissions = submissionService.getSubmissionsByAssignmentId(assignmentId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Submissions retrieved successfully");
        response.put("submissions", submissions);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadSubmission(@RequestParam("submissionId") String submissionId) throws IOException {
        if (submissionId == null || submissionId.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Submission ID cannot be null or blank"));
        }

        GridFSFile gridFSFile = fileService.getFileBySubmissionId(submissionId);
        if (gridFSFile == null) {
            return ResponseEntity.status(404)
                    .body(new ErrorResponse("File not found for submission ID: " + submissionId));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "File retrieved successfully");
        response.put("filename", gridFSFile.getFilename());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(gridFSFile.getMetadata().getString("_contentType")))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + gridFSFile.getFilename() + "\"")
                .body(new InputStreamResource(
                        fileService.getGridFsTemplate().getResource(gridFSFile).getInputStream()));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteSubmission(@RequestBody DeleteSubmissionRequest request) {
        logger.info("Received delete submission request: {}", request);
        String assignmentId = request.getAssignmentId();
        String studentRollNumber = request.getStudentRollNumber();
        if (assignmentId == null || assignmentId.isBlank() || studentRollNumber == null || studentRollNumber.isBlank()) {
            logger.warn("Invalid request: assignmentId or studentRollNumber is null or blank");
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Assignment ID and User ID cannot be null or blank"));
        }

        try {
            submissionService.deleteSubmissionByAssignmentIdAndStudentRollNumber(assignmentId, studentRollNumber);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Submission deleted successfully");
            response.put("assignmentId", assignmentId);
            response.put("studentRollNumber", studentRollNumber);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to delete submission for studentRollNumber: {}, assignmentId: {}, reason: {}", studentRollNumber, assignmentId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to delete submission: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error deleting submission for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId, e);
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/id")
    public ResponseEntity<?> getSubmissionById(@RequestParam("submissionId") String submissionId) {
        if (submissionId == null || submissionId.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Submission ID cannot be null or blank"));
        }

        try {
            Submission submission = submissionService.getSubmissionById(submissionId);
            if (submission == null) {
                return ResponseEntity.status(404).body(new ErrorResponse("Submission not found"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Submission retrieved successfully");
            response.put("submission", submission);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/progress")
    public ResponseEntity<?> getStudentProgress(@RequestParam("studentRollNumber") String studentRollNumber,
                                               @RequestParam("courseId") String courseId) {
        if (studentRollNumber == null || studentRollNumber.isBlank() || courseId == null || courseId.isBlank()) {
            logger.warn("Invalid request: studentRollNumber or courseId is null or blank");
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Student Roll Number and Course ID cannot be null or blank"));
        }

        try {
            List<Assignment> assignments = assignmentService.getAssignmentsByCourseId(courseId);
            long totalAssignments = assignments.size();
            if (totalAssignments == 0) {
                logger.warn("No assignments found for courseId: {}", courseId);
                return ResponseEntity.ok()
                        .body(Map.of(
                            "message", "No assignments found for the course",
                            "studentRollNumber", studentRollNumber,
                            "courseId", courseId,
                            "progressPercentage", 0.0
                        ));
            }

            List<String> assignmentIds = assignments.stream()
                    .map(Assignment::getAssignmentId)
                    .toList();

            long submissionCount = submissionService.countByStudentRollNumberAndAssignmentIds(studentRollNumber, assignmentIds);
            double progressPercentage = (double) submissionCount / totalAssignments * 100;
            progressPercentage = Math.round(progressPercentage * 100.0) / 100.0;

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Progress calculated successfully");
            response.put("studentRollNumber", studentRollNumber);
            response.put("courseId", courseId);
            response.put("progressPercentage", progressPercentage);
            logger.info("Calculated progress {}% for studentRollNumber: {}, courseId: {}", 
                progressPercentage, studentRollNumber, courseId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error calculating progress for studentRollNumber: {}, courseId: {}", studentRollNumber, courseId, e);
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    @GetMapping("/grade/average")
    public ResponseEntity<?> getStudentAverageGrade(@RequestParam("studentRollNumber") String studentRollNumber,
                                                   @RequestParam("courseId") String courseId) {
        if (studentRollNumber == null || studentRollNumber.isBlank() || courseId == null || courseId.isBlank()) {
            logger.warn("Invalid request: studentRollNumber or courseId is null or blank");
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Student Roll Number and Course ID cannot be null or blank"));
        }

        try {
            List<Assignment> assignments = assignmentService.getAssignmentsByCourseId(courseId);
            if (assignments.isEmpty()) {
                logger.warn("No assignments found for courseId: {}", courseId);
                return ResponseEntity.ok()
                        .body(Map.of(
                            "message", "No assignments found for the course",
                            "studentRollNumber", studentRollNumber,
                            "courseId", courseId,
                            "averageGrade", null
                        ));
            }

            List<String> assignmentIds = assignments.stream()
                    .map(Assignment::getAssignmentId)
                    .toList();

            double totalGrade = 0.0;
            int gradedAssignments = 0;
            for (String assignmentId : assignmentIds) {
                Optional<com.assignmentservice.assignmentservice.Model.Grading> gradingOpt = gradingRepository.findByStudentRollNumberAndAssignmentId(studentRollNumber, assignmentId);
                if (gradingOpt.isPresent()) {
                    String grade = gradingOpt.get().getGrade();
                    if (grade != null && !grade.isBlank()) {
                        totalGrade += convertLetterGradeToNumber(grade);
                        gradedAssignments++;
                    }
                }
            }

            Double averageGrade = gradedAssignments > 0 ? Math.round((totalGrade / gradedAssignments) * 100.0) / 100.0 : null;

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Average grade calculated successfully");
            response.put("studentRollNumber", studentRollNumber);
            response.put("courseId", courseId);
            response.put("averageGrade", averageGrade != null ? averageGrade : "Not graded");
            logger.info("Calculated average grade {} for studentRollNumber: {}, courseId: {}", 
                averageGrade != null ? averageGrade : "Not graded", studentRollNumber, courseId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Unexpected error calculating average grade for studentRollNumber: {}, courseId: {}", studentRollNumber, courseId, e);
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    private double convertLetterGradeToNumber(String grade) {
        if (grade == null || grade.isBlank()) {
            logger.error("Grade is null or blank");
            throw new IllegalArgumentException("Grade cannot be null or blank");
        }
        switch (grade.toUpperCase()) {
            case "A+": return 95.0;
            case "A": return 90.0;
            case "B+": return 85.0;
            case "B": return 80.0;
            case "C+": return 75.0;
            case "C": return 70.0;
            case "D+": return 65.0;
            case "D": return 60.0;
            case "F": return 0.0;
            default:
                logger.error("Invalid grade format: {}", grade);
                throw new IllegalArgumentException("Invalid grade format: " + grade);
        }
    }

    @Data
    public static class UpdateSubmissionStatusRequest {
        private String submissionId;
        private String status;
        private String assignmentId;
    }

    @Data
    public static class DeleteSubmissionRequest {
        private String assignmentId;
        private String studentRollNumber;
    }

    @Data
    public static class SubmissionIdRequest {
        private String submissionId;
    }

    @Data
    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }
    }
}