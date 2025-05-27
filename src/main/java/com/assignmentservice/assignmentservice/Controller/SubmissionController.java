package com.assignmentservice.assignmentservice.Controller;

import com.assignmentservice.assignmentservice.Model.Submission;
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
import com.assignmentservice.assignmentservice.Model.StudentProgress;

@RestController
@CrossOrigin(origins = "http://localhost:8080")
@RequestMapping("/api/submissions")
public class SubmissionController {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionController.class);

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private FileService fileService;

    @Autowired
    private com.assignmentservice.assignmentservice.Service.StudentProgressService studentProgressService;

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
            Optional<StudentProgress> progressOpt = studentProgressService.getProgressByStudentAndCourse(studentRollNumber, courseId);
            double progressPercentage;
            if (progressOpt.isPresent()) {
                progressPercentage = progressOpt.get().getProgressPercentage();
                logger.info("Retrieved cached progress {}% for studentRollNumber: {}, courseId: {}", progressPercentage, studentRollNumber, courseId);
            } else {
                progressPercentage = studentProgressService.calculateProgress(studentRollNumber, courseId);
                logger.info("Calculated progress {}% for studentRollNumber: {}, courseId: {}", progressPercentage, studentRollNumber, courseId);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Progress retrieved successfully");
            response.put("studentRollNumber", studentRollNumber);
            response.put("courseId", courseId);
            response.put("progress", progressPercentage);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to retrieve progress for studentRollNumber: {}, courseId: {}, reason: {}", studentRollNumber, courseId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to retrieve progress: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving progress for studentRollNumber: {}, courseId: {}", studentRollNumber, courseId, e);
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
            Optional<StudentProgress> progressOpt = studentProgressService.getProgressByStudentAndCourse(studentRollNumber, courseId);
            Double averageGrade;
            if (progressOpt.isPresent() && progressOpt.get().getAverageGrade() != null) {
                averageGrade = progressOpt.get().getAverageGrade();
                logger.info("Retrieved cached average grade {} for studentRollNumber: {}, courseId: {}", averageGrade, studentRollNumber, courseId);
            } else {
                averageGrade = studentProgressService.calculateAverageGrade(studentRollNumber, courseId);
                logger.info("Calculated average grade {} for studentRollNumber: {}, courseId: {}", averageGrade != null ? averageGrade : "Not graded", studentRollNumber, courseId);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Average grade retrieved successfully");
            response.put("studentRollNumber", studentRollNumber);
            response.put("courseId", courseId);
            response.put("averageGrade", averageGrade != null ? averageGrade : "Not graded");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to retrieve average grade for studentRollNumber: {}, courseId: {}, reason: {}", studentRollNumber, courseId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to retrieve average grade: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error retrieving average grade for studentRollNumber: {}, courseId: {}", studentRollNumber, courseId, e);
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }

    @Data
    public static class DeleteSubmissionRequest {
        private String assignmentId;
        private String studentRollNumber;
    }

    @Data
    public static class AssignmentIdRequest {
        private String assignmentId;
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