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

    private static final Map<String, Double> GRADE_MAP = Map.of(
            "O", 100.0,
            "A+", 90.0,
            "A", 80.0,
            "B+", 70.0,
            "B", 60.0,
            "C+", 50.0,
            "C", 40.0
    );

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> submitAssignment(
            @RequestParam("assignmentId") String assignmentId,
            @RequestParam("studentName") String studentName,
            @RequestParam("studentRollNumber") String studentRollNumber,
            @RequestParam("studentDepartment") String studentDepartment,
            @RequestParam("studentSemester") String studentSemester,
            @RequestPart("file") MultipartFile file) throws IOException {
        Submission submission = submissionService.saveSubmission(assignmentId, studentName, studentRollNumber, studentDepartment, studentSemester, file);
        return ResponseEntity.ok(Map.of(
                "message", "Submission created successfully",
                "submissionId", submission.getId()
        ));
    }

    @PostMapping("/status")
    public ResponseEntity<?> updateSubmissionStatus(@RequestBody UpdateSubmissionStatusRequest request) {
        Assignment assignment = assignmentService.getAssignmentById(
                Optional.ofNullable(request.getAssignmentId())
                        .filter(id -> !id.isBlank())
                        .orElseThrow(() -> new IllegalArgumentException("Assignment ID cannot be null or blank"))
        ).orElseThrow(() -> new IllegalArgumentException("Assignment not found for ID: " + request.getAssignmentId()));
        Submission submission = submissionService.updateSubmissionStatus(
                Optional.ofNullable(request.getSubmissionId())
                        .filter(id -> !id.isBlank())
                        .orElseThrow(() -> new IllegalArgumentException("Submission ID cannot be null or blank")),
                Optional.ofNullable(request.getStatus())
                        .filter(status -> List.of("Accepted", "Rejected").contains(status))
                        .orElseThrow(() -> new IllegalArgumentException("Status must be either 'Accepted' or 'Rejected'")),
                assignment.getTitle()
        );
        return ResponseEntity.ok(Map.of(
                "message", "Submission status updated to " + request.getStatus(),
                "submission", submission
        ));
    }

    @GetMapping
    public ResponseEntity<?> getSubmissionsByAssignmentId(@RequestParam("assignmentId") String assignmentId) {
        List<Submission> submissions = submissionService.getSubmissionsByAssignmentId(
                Optional.ofNullable(assignmentId)
                        .filter(id -> !id.isBlank())
                        .orElseThrow(() -> new IllegalArgumentException("Assignment ID cannot be null or blank"))
        );
        return ResponseEntity.ok(Map.of(
                "message", "Submissions retrieved successfully",
                "submissions", submissions
        ));
    }

    @GetMapping("/download")
    public ResponseEntity<?> downloadSubmission(@RequestParam("submissionId") String submissionId) throws IOException {
        GridFSFile gridFSFile = Optional.ofNullable(submissionId)
                .filter(id -> !id.isBlank())
                .map(fileService::getFileBySubmissionId)
                .orElseThrow(() -> new IllegalArgumentException("File not found for submission ID: " + submissionId));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(gridFSFile.getMetadata().getString("_contentType")))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + gridFSFile.getFilename() + "\"")
                .body(new InputStreamResource(
                        fileService.getGridFsTemplate().getResource(gridFSFile).getInputStream()));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteSubmission(@RequestBody DeleteSubmissionRequest request) {
        logger.info("Received delete submission request: {}", request);
        String assignmentId = Optional.ofNullable(request.getAssignmentId())
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Assignment ID cannot be null or blank"));
        String studentRollNumber = Optional.ofNullable(request.getStudentRollNumber())
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Student Roll Number cannot be null or blank"));
        submissionService.deleteSubmissionByAssignmentIdAndStudentRollNumber(assignmentId, studentRollNumber);
        return ResponseEntity.ok(Map.of(
                "message", "Submission deleted successfully",
                "assignmentId", assignmentId,
                "studentRollNumber", studentRollNumber
        ));
    }

    @GetMapping("/id")
    public ResponseEntity<?> getSubmissionById(@RequestParam("submissionId") String submissionId) {
        Submission submission = Optional.ofNullable(submissionId)
                .filter(id -> !id.isBlank())
                .map(submissionService::getSubmissionById)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found for ID: " + submissionId));
        return ResponseEntity.ok(Map.of(
                "message", "Submission retrieved successfully",
                "submission", submission
        ));
    }

    @GetMapping("/progress")
    public ResponseEntity<?> getStudentProgress(@RequestParam("studentRollNumber") String studentRollNumber,
                                               @RequestParam("courseId") String courseId) {
        String validRollNumber = Optional.ofNullable(studentRollNumber)
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Student Roll Number cannot be null or blank"));
        String validCourseId = Optional.ofNullable(courseId)
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Course ID cannot be null or blank"));

        List<Assignment> assignments = assignmentService.getAssignmentsByCourseId(validCourseId);
        long totalAssignments = assignments.size();
        double progressPercentage = assignments.isEmpty() ? 0.0 :
                submissionService.countByStudentRollNumberAndAssignmentIds(
                        validRollNumber,
                        assignments.stream().map(Assignment::getAssignmentId).toList()
                ) * 100.0 / totalAssignments;
        progressPercentage = Math.round(progressPercentage * 100.0) / 100.0;

        logger.info("Calculated progress {}% for studentRollNumber: {}, courseId: {}",
                progressPercentage, validRollNumber, validCourseId);
        return ResponseEntity.ok(Map.of(
                "message", assignments.isEmpty() ? "No assignments found for the course" : "Progress calculated successfully",
                "studentRollNumber", validRollNumber,
                "courseId", validCourseId,
                "progressPercentage", progressPercentage
        ));
    }

    @GetMapping("/courses/{courseId}/student-progress")
    public ResponseEntity<?> getAllStudentProgress(@PathVariable String courseId) {
        String validCourseId = Optional.ofNullable(courseId)
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Course ID cannot be null or blank"));

        List<SubmissionService.StudentProgress> progressList = submissionService.getStudentProgressForCourse(validCourseId);
        logger.info("Retrieved progress for {} students for courseId: {}", progressList.size(), validCourseId);
        return ResponseEntity.ok(Map.of(
                "message", "Student progress retrieved successfully",
                "courseId", validCourseId,
                "students", progressList
        ));
    }

    @GetMapping("/grade/average")
    public ResponseEntity<?> getStudentAverageGrade(@RequestParam("studentRollNumber") String studentRollNumber,
                                                   @RequestParam("courseId") String courseId) {
        String validRollNumber = Optional.ofNullable(studentRollNumber)
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Student Roll Number cannot be null or blank"));
        String validCourseId = Optional.ofNullable(courseId)
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Course ID cannot be null or blank"));

        List<Assignment> assignments = assignmentService.getAssignmentsByCourseId(validCourseId);
        List<String> assignmentIds = assignments.stream()
                .map(Assignment::getAssignmentId)
                .toList();

        double totalGrade = assignmentIds.stream()
                .map(id -> gradingRepository.findByStudentRollNumberAndAssignmentId(validRollNumber, id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(grading -> grading.getGrade())
                .filter(grade -> grade != null && !grade.isBlank())
                .mapToDouble(this::convertLetterGradeToNumber)
                .sum();

        long gradedAssignments = assignmentIds.stream()
                .map(id -> gradingRepository.findByStudentRollNumberAndAssignmentId(validRollNumber, id))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(grading -> grading.getGrade())
                .filter(grade -> grade != null && !grade.isBlank())
                .count();

        Double averageGrade = gradedAssignments > 0 ? Math.round((totalGrade / gradedAssignments) * 100.0) / 100.0 : null;

        logger.info("Calculated average grade {} for studentRollNumber: {}, courseId: {}",
                averageGrade != null ? averageGrade : "Not graded", validRollNumber, validCourseId);
        return ResponseEntity.ok(Map.of(
                "message", assignments.isEmpty() ? "No assignments found for the course" : "Average grade calculated successfully",
                "studentRollNumber", validRollNumber,
                "courseId", validCourseId,
                "averageGrade", averageGrade != null ? averageGrade : "Not graded"
        ));
    }

    private double convertLetterGradeToNumber(String grade) {
        return Optional.ofNullable(grade)
                .filter(g -> !g.isBlank())
                .map(String::toUpperCase)
                .map(GRADE_MAP::get)
                .orElseThrow(() -> new IllegalArgumentException("Invalid grade: " + grade));
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
    public static class ErrorResponse {
        private String message;

        public ErrorResponse(String message) {
            this.message = message;
        }
    }
}