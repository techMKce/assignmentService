
package com.assignmentservice.assignmentservice.Controller;

import com.assignmentservice.assignmentservice.Model.Assignment;
import com.assignmentservice.assignmentservice.Service.AssignmentService;
import com.assignmentservice.assignmentservice.Service.FileService;
import com.mongodb.client.gridfs.model.GridFSFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/assignments")
public class AssignmentController {

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private FileService fileService;

    @PostMapping(value = "/createassignment", consumes = "multipart/form-data")
    public ResponseEntity<?> createAssignment(
            @RequestParam("CourseId") String CourseId,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("dueDate") String dueDate,
            @RequestPart("file") MultipartFile file) throws IOException {
        try {

            Assignment assignment = new Assignment();
            assignment.setCourseId(CourseId);
            assignment.setTitle(title);
            assignment.setDescription(description);

            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            assignment.setDueDate(LocalDateTime.parse(dueDate, formatter));

            String assignmentId = UUID.randomUUID().toString();
            assignment.setAssignmentId(assignmentId);

            String fileNo = fileService.uploadFile(file, assignmentId);
            assignment.setFileNo(fileNo);
            assignmentService.saveAssignment(assignment);

            return ResponseEntity.ok(assignmentId);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to create assignment: " + e.getMessage()));
        }
    }

    @PostMapping("/downloadfile")
    public ResponseEntity<?> downloadFile(@RequestBody AssignmentIdRequest idRequest) throws IOException {
        String assignmentId = idRequest.getAssignmentId();
        if (assignmentId == null || assignmentId.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Assignment ID cannot be null or blank"));
        }

        GridFSFile gridFSFile = fileService.getFileByAssignmentId(assignmentId);
        if (gridFSFile == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(gridFSFile.getMetadata().getString("_contentType")))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + gridFSFile.getFilename() + "\"")
                .body(new InputStreamResource(
                        fileService.getGridFsTemplate().getResource(gridFSFile).getInputStream()));
    }

    @PutMapping("/updateassignment")
    public ResponseEntity<?> updateAssignment(@Valid @RequestBody Assignment assignment) {
        String id = assignment.getAssignmentId();
        if (id == null || id.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Assignment ID cannot be null or blank"));
        }
        Optional<Assignment> existingAssignment = assignmentService.getAssignmentById(id);
        if (existingAssignment.isPresent()) {
            Assignment updatedAssignment = assignmentService.saveAssignment(assignment);
            return ResponseEntity.ok(updatedAssignment);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/deleteassignment")
    public ResponseEntity<Void> deleteAssignment(@RequestBody AssignmentIdRequest idRequest) {
        String id = idRequest.getAssignmentId();
        if (id == null || id.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Optional<Assignment> existingAssignment = assignmentService.getAssignmentById(id);
        if (existingAssignment.isPresent()) {
            assignmentService.deleteAssignment(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<Assignment>> getAllAssignments() {
        List<Assignment> assignments = assignmentService.getAllAssignments();
        return ResponseEntity.ok(assignments);
    }
}

class ErrorResponse {
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

class AssignmentIdRequest {
    private String assignmentId;

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }
}
