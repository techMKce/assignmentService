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
@RequestMapping("/api/v1/assignments")
public class AssignmentController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AssignmentController.class);

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
            @RequestParam("resourceLink") String resourceLink) throws IOException {
        try {
            logger.info("Creating assignment with courseId: {}, courseName: {}, courseFaculty: {}",
                    courseId, courseName, courseFaculty);
            if (courseId == null || courseId.isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Course ID cannot be null or blank"));
            }
            if (courseName == null || courseName.isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Course name cannot be null or blank"));
            }
            if (courseFaculty == null || courseFaculty.isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Course faculty cannot be null or blank"));
            }
            if (title == null || title.isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Title cannot be null or blank"));
            }
            if (description == null || description.isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Description cannot be null or blank"));
            }
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("File cannot be null or empty"));
            }

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
                        .body(new ErrorResponse(
                                "Invalid dueDate format; use ISO_LOCAL_DATE_TIME (e.g., 2025-06-01T23:59:00)"));
            }

            String assignmentId = UUID.randomUUID().toString();
            assignment.setAssignmentId(assignmentId);

            String fileNo = fileService.uploadFile(file, assignmentId);
            assignment.setFileNo(fileNo);
            assignment.setFileName(file.getOriginalFilename());
            logger.info("Set fileNo: {}, fileName: {} for assignmentId: {}", fileNo, file.getOriginalFilename(),
                    assignmentId);
            assignmentService.saveAssignment(assignment);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Assignment created successfully");
            response.put("assignmentId", assignmentId);
            logger.info("Assignment created successfully with ID: {}", assignmentId);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            logger.error("Failed to create assignment due to file upload error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to upload file: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to create assignment: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to create assignment: " + e.getMessage()));
        }
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
            @RequestParam(value = "resourceLink", required =false) String resourceLink,
            @RequestPart(value = "file", required = false) MultipartFile file) throws IOException {
        try {
            logger.info("Updating assignment with ID: {}, courseName: {}, courseFaculty: {}",
                    assignmentId, courseName, courseFaculty);
            if (assignmentId == null || assignmentId.isBlank()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Assignment ID cannot be null or blank"));
            }
            Optional<Assignment> existingAssignmentOpt = assignmentService.getAssignmentById(assignmentId);
            if (!existingAssignmentOpt.isPresent()) {
                return ResponseEntity.status(404)
                        .body(new ErrorResponse("Assignment not found for ID: " + assignmentId));
            }

            Assignment existingAssignment = existingAssignmentOpt.get();

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
            if (resourceLink !=null && !resourceLink.isBlank()) {
                existingAssignment.setResourceLink(resourceLink);
            }
            if (dueDate != null && !dueDate.isBlank()) {
                DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                try {
                    existingAssignment.setDueDate(LocalDateTime.parse(dueDate, formatter));
                } catch (DateTimeParseException e) {
                    return ResponseEntity.badRequest()
                            .body(new ErrorResponse(
                                    "Invalid dueDate format; use ISO_LOCAL_DATE_TIME (e.g., 2025-06-01T23:59:00)"));
                }
            }
            if (file != null && !file.isEmpty()) {
                if (existingAssignment.getFileNo() != null) {
                    fileService.deleteFileByFileNo(existingAssignment.getFileNo());
                    logger.info("Deleted old file with fileNo: {} for assignmentId: {}",
                            existingAssignment.getFileNo(), assignmentId);
                }
                String newFileNo = fileService.uploadFile(file, assignmentId);
                existingAssignment.setFileNo(newFileNo);
                existingAssignment.setFileName(file.getOriginalFilename());
                logger.info("Set new fileNo: {}, fileName: {} for assignmentId: {}",
                        newFileNo, file.getOriginalFilename(), assignmentId);
            }

            Assignment updatedAssignment = assignmentService.saveAssignment(existingAssignment);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Assignment updated successfully");
            response.put("assignment", updatedAssignment);
            logger.info("Assignment updated successfully with ID: {}", assignmentId);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            logger.error("Failed to update assignment due to file upload error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to upload file: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to update assignment: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Failed to update assignment: " + e.getMessage()));
        }
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadFile(@RequestParam("assignmentId") String assignmentId) throws IOException {
        logger.info("Downloading file for assignmentId: {}", assignmentId);

        if (assignmentId == null || assignmentId.isBlank()) {
            logger.error("Assignment ID is null or blank");
            return ResponseEntity.badRequest().body(new ErrorResponse("Assignment ID cannot be null or blank"));
        }

        Optional<Assignment> assignmentOpt = assignmentService.getAssignmentById(assignmentId);
        if (!assignmentOpt.isPresent()) {
            logger.error("Assignment not found for ID: {}", assignmentId);
            return ResponseEntity.status(404)
                    .body(new ErrorResponse("Assignment not found for ID: " + assignmentId));
        }

        String fileNo = assignmentOpt.get().getFileNo();
        if (fileNo == null || fileNo.isBlank()) {
            logger.error("No fileNo associated with assignmentId: {}", assignmentId);
            return ResponseEntity.status(404)
                    .body(new ErrorResponse("No file associated with assignment ID: " + assignmentId));
        }

        logger.info("Retrieving GridFS file with fileNo: {}", fileNo);
        GridFSFile gridFSFile = fileService.getFileByFileNo(fileNo);
        if (gridFSFile == null) {
            logger.error("File not found in GridFS for fileNo: {}", fileNo);
            return ResponseEntity.status(404)
                    .body(new ErrorResponse("File not found for fileNo: " + fileNo));
        }

        logger.info("Found file: filename={}, contentType={}",
                gridFSFile.getFilename(),
                gridFSFile.getMetadata().getString("_contentType"));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(gridFSFile.getMetadata().getString("_contentType")))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + gridFSFile.getFilename() + "\"")
                .body(new InputStreamResource(
                        fileService.getGridFsTemplate().getResource(gridFSFile).getInputStream()));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAssignment(@RequestParam("assignmentId") String id) {
        if (id == null || id.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Assignment ID cannot be null or blank"));
        }
        Optional<Assignment> existingAssignment = assignmentService.getAssignmentById(id);
        if (existingAssignment.isPresent()) {
            assignmentService.deleteAssignment(id);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Assignment deleted successfully");
            response.put("assignmentId", id);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(404)
                .body(new ErrorResponse("Assignment not found for ID: " + id));
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
            return ResponseEntity.badRequest().body(new ErrorResponse("Assignment ID cannot be null or blank"));
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
            return ResponseEntity.badRequest().body(new ErrorResponse("Course ID cannot be null or blank"));
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