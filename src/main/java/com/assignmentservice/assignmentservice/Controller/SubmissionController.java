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
@RequestMapping("/api/submissions")
public class SubmissionController {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionController.class);

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private FileService fileService;

    @PostMapping(value = "/submitassignment", consumes = "multipart/form-data")
    public ResponseEntity<?> submitAssignment(
            @RequestParam("userId") String userId,
            @RequestParam("assignmentId") String assignmentId,
            @RequestParam("studentName") String studentName,
            @RequestParam("studentRollNumber") String studentRollNumber,
            @RequestPart("file") MultipartFile file) throws IOException {
        try {
            Submission submission = submissionService.saveSubmission(userId, assignmentId, studentName, studentRollNumber, file);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Submission created successfully");
            response.put("submissionId", submission.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to submit assignment for userId: {}, assignmentId: {}", userId, assignmentId, e);
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to submit assignment: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllSubmission() {
        List<Submission> submissions = submissionService.getAllSubmission();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Submissions retrieved successfully");
        response.put("submissions", submissions);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/assignment")
    public ResponseEntity<?> getSubmissionsByAssignmentId(@RequestBody AssignmentIdRequest request) {
        String assignmentId = request.getAssignmentId();
        if (assignmentId == null || assignmentId.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Assignment ID cannot be null or blank"));
        }
        List<Submission> submissions = submissionService.getSubmissionsByAssignmentId(assignmentId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Submissions retrieved successfully");
        response.put("submissions", submissions);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/downloadsubmission")
    public ResponseEntity<?> downloadSubmission(@RequestBody SubmissionIdRequest idRequest) throws IOException {
        String submissionId = idRequest.getSubmissionId();
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
    @DeleteMapping("/deletesubmission")
    public ResponseEntity<?> deleteSubmission(@RequestBody DeleteSubmissionRequest request) {
        logger.info("Received delete submission request: {}", request);
        String assignmentId = request.getAssignmentId();
        String userId = request.getUserId();
        if (assignmentId == null || assignmentId.isBlank() || userId == null || userId.isBlank()) {
            logger.warn("Invalid request: assignmentId or userId is null or blank");
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Assignment ID and User ID cannot be null or blank"));
        }

        try {
            submissionService.deleteSubmissionByAssignmentIdAndUserId(assignmentId, userId);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Submission deleted successfully");
            response.put("assignmentId", assignmentId);
            response.put("userId", userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to delete submission for userId: {}, assignmentId: {}, reason: {}", userId, assignmentId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to delete submission: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error deleting submission for userId: {}, assignmentId: {}", userId, assignmentId, e);
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Internal server error: " + e.getMessage()));
        }
    }
    @Data
    public static class DeleteSubmissionRequest {
        private String assignmentId;
        private String userId;
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