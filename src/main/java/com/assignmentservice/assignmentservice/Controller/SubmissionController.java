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

@RestController
@CrossOrigin(origins = "http://localhost:8080")
@RequestMapping("/api/submissions")
public class SubmissionController {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionController.class);

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private FileService fileService;

    @PostMapping( consumes = "multipart/form-data")
    public ResponseEntity<?> submitAssignment(
            @RequestParam("assignmentId") String assignmentId,
            @RequestParam("studentName") String studentName,
            @RequestParam("studentRollNumber") String studentRollNumber,
            @RequestPart("file") MultipartFile file) throws IOException {
        try {
            Submission submission = submissionService.saveSubmission( assignmentId, studentName, studentRollNumber, file);
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