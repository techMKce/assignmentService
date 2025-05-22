
package com.assignmentservice.assignmentservice.Controller;

import com.assignmentservice.assignmentservice.Model.Submission;
import com.assignmentservice.assignmentservice.Service.SubmissionService;
import com.assignmentservice.assignmentservice.Service.FileService;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/submissions")
public class SubmissionController {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private FileService fileService;

    @PostMapping(value = "/submitassignment", consumes = "multipart/form-data")
    public ResponseEntity<?> submitAssignment(
            @RequestParam("userId") String userId,
            @RequestParam("assignmentId") String assignmentId,
            @RequestPart("file") MultipartFile file) throws IOException {
        try {
            Submission submission = submissionService.saveSubmission(userId, assignmentId, file);
            return ResponseEntity.ok(submission.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to submit assignment: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Submission>> getAllSubmission() {
        List<Submission> submissions = submissionService.getAllSubmission();
        return ResponseEntity.ok(submissions);
    }

    @PostMapping("/assignment")
    public ResponseEntity<?> getSubmissionsByAssignmentId(@RequestBody AssignmentIdRequest request) {
        String assignmentId = request.getAssignmentId();
        if (assignmentId == null || assignmentId.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Assignment ID cannot be null or blank"));
        }
        List<Submission> submissions = submissionService.getSubmissionsByAssignmentId(assignmentId);
        return ResponseEntity.ok(submissions);
    }

    @PostMapping("/downloadsubmission")
    public ResponseEntity<?> downloadSubmission(@RequestBody SubmissionIdRequest idRequest) throws IOException {
        String submissionId = idRequest.getSubmissionId();
        if (submissionId == null || submissionId.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Submission ID cannot be null or blank"));
        }

        GridFSFile gridFSFile = fileService.getFileBySubmissionId(submissionId);
        if (gridFSFile == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(gridFSFile.getMetadata().getString("_contentType")))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + gridFSFile.getFilename() + "\"")
                .body(new InputStreamResource(
                        fileService.getGridFsTemplate().getResource(gridFSFile).getInputStream()));
    }

    @DeleteMapping("/deleteassignment")
    public ResponseEntity<?> deleteSubmission(@RequestBody DeleteSubmissionRequest request) {
        String assignmentId = request.getAssignmentId();
        String userId = request.getUserId();
        if (assignmentId == null || assignmentId.isBlank() || userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Assignment ID and User ID cannot be null or blank"));
        }

        submissionService.deleteSubmissionByAssignmentIdAndUserId(assignmentId, userId);
        return ResponseEntity.noContent().build();
    }

    public static class DeleteSubmissionRequest {
        private String assignmentId;
        private String userId;

        public String getAssignmentId() {
            return assignmentId;
        }

        public void setAssignmentId(String assignmentId) {
            this.assignmentId = assignmentId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
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

    public static class SubmissionIdRequest {
        private String submissionId;

        public String getSubmissionId() {
            return submissionId;
        }

        public void setSubmissionId(String submissionId) {
            this.submissionId = submissionId;
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
