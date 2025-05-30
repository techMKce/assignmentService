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

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:8080")
@RequestMapping("/api/assignments")
public class AssignmentController {

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private FileService fileService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> createAssignment(
            @RequestParam("courseId") String courseId,
            @RequestParam("courseName") String courseName,
            @RequestParam("courseFaculty") String courseFaculty,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("dueDate") String dueDate,
            @RequestPart("file") MultipartFile file,
            @RequestParam("resourceLink") String resourceLink) {
        if (courseId == null || courseId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Course ID cannot be null or blank"));
        }
        if (courseName == null || courseName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Course Name cannot be null or blank"));
        }
        if (courseFaculty == null || courseFaculty.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Course Faculty cannot be null or blank"));
        }
        try {
            Assignment assignment = new Assignment();
            assignment.setCourseId(courseId);
            assignment.setCourseName(courseName);
            assignment.setCourseFaculty(courseFaculty);
            assignment.setTitle(title);
            assignment.setDescription(description);
            assignment.setResourceLink(resourceLink);

            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            try {
                assignment.setDueDate(LocalDateTime.parse(dueDate, formatter));
            } catch (DateTimeParseException e) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid dueDate format; use ISO_LOCAL_DATE_TIME (e.g., 2025-06-01T23:59:00)"));
            }

            String assignmentId = UUID.randomUUID().toString();
            assignment.setAssignmentId(assignmentId);

            String fileName = file.getOriginalFilename();
            fileService.uploadFile(file, assignmentId);
            assignment.setFileName(fileName);
            Assignment savedAssignment = assignmentService.saveAssignment(assignment);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Assignment created successfully");
            response.put("assignment", savedAssignment);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to upload file: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to create assignment: " + e.getMessage()));
        }
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(@RequestParam("assignmentId") String assignmentId) throws IOException {
        if (assignmentId == null || assignmentId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Assignment ID cannot be null or blank"));
        }
        GridFSFile gridFSFile = fileService.getFileByAssignmentId(assignmentId);
        if (gridFSFile == null) {
            return ResponseEntity.status(404)
                    .body(new ErrorResponse("File not found for assignment ID: " + assignmentId));
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(gridFSFile.getMetadata().getString("_contentType")))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + gridFSFile.getFilename() + "\"")
                .body(new InputStreamResource(
                        fileService.getGridFsTemplate().getResource(gridFSFile).getInputStream()));
    }

    @PutMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> updateAssignment(
            @RequestParam("assignmentId") String assignmentId,
            @RequestParam(value = "courseId", required = false) String courseId,
            @RequestParam(value = "courseName", required = false) String courseName,
            @RequestParam(value = "courseFaculty", required = false) String courseFaculty,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "dueDate", required = false) String dueDate,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        if (assignmentId == null || assignmentId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Assignment ID cannot be null or blank"));
        }
        Optional<Assignment> existingAssignmentOpt = assignmentService.getAssignmentById(assignmentId);
        if (!existingAssignmentOpt.isPresent()) {
            return ResponseEntity.status(404)
                    .body(new ErrorResponse("Assignment not found for ID: " + assignmentId));
        }

        Assignment existingAssignment = existingAssignmentOpt.get();
        try {
            if (courseId != null && !courseId.isBlank()) {
                existingAssignment.setCourseId(courseId);
            }
            if (courseName != null && !courseName.isBlank()) {
                existingAssignment.setCourseName(courseName);
            }
            if (courseFaculty != null && !courseFaculty.isBlank()) {
                existingAssignment.setCourseFaculty(courseFaculty);
            }
            if (title != null && !title.isBlank()) {
                existingAssignment.setTitle(title);
            }
            if (description != null && !description.isBlank()) {
                existingAssignment.setDescription(description);
            }
            if (dueDate != null && !dueDate.isBlank()) {
                try {
                    existingAssignment.setDueDate(LocalDateTime.parse(dueDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                } catch (DateTimeParseException e) {
                    return ResponseEntity.badRequest()
                            .body(new ErrorResponse("Invalid dueDate format; use ISO_LOCAL_DATE_TIME (e.g., 2025-06-01T23:59:00)"));
                }
            }
            if (file != null && !file.isEmpty()) {
                if (existingAssignment.getFileName() != null) {
                    fileService.deleteFileByFileName(existingAssignment.getFileName());
                }
                String newFileName = file.getOriginalFilename();
                fileService.uploadFile(file, assignmentId);
                existingAssignment.setFileName(newFileName);
            }

            Assignment updatedAssignment = assignmentService.saveAssignment(existingAssignment);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Assignment updated successfully");
            response.put("assignment", updatedAssignment);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to upload file: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to update assignment: " + e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAssignment(@RequestParam("assignmentId") String id) {
        if (id == null || id.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Assignment ID cannot be null or blank"));
        }
        Optional<Assignment> existingAssignment = assignmentService.getAssignmentById(id);
        if (!existingAssignment.isPresent()) {
            return ResponseEntity.status(404)
                    .body(new ErrorResponse("Assignment not found for ID: " + id));
        }
        assignmentService.deleteAssignment(id);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Assignment deleted successfully");
        response.put("assignmentId", id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllAssignments() {
        List<Assignment> assignments = assignmentService.getAllAssignments();
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Assignments retrieved successfully");
        response.put("assignments", assignments);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/id")
    public ResponseEntity<?> getAssignmentById(@RequestParam("assignmentId") String assignmentId) {
        if (assignmentId == null || assignmentId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Assignment ID cannot be null or blank"));
        }
        Optional<Assignment> assignmentOpt = assignmentService.getAssignmentById(assignmentId);
        if (!assignmentOpt.isPresent()) {
            return ResponseEntity.status(404)
                    .body(new ErrorResponse("Assignment not found for ID: " + assignmentId));
        }
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Assignment retrieved successfully");
        response.put("assignment", assignmentOpt.get());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/course")
    public ResponseEntity<?> getAssignmentsByCourseId(@RequestParam("courseId") String courseId) {
        if (courseId == null || courseId.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Course ID cannot be null or blank"));
        }
        List<Assignment> assignments = assignmentService.getAssignmentsByCourseId(courseId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Assignments retrieved successfully for course ID: " + courseId);
        response.put("assignments", assignments);
        return ResponseEntity.ok(response);
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

class CourseIdRequest {
    private String courseId;

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }
}