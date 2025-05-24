package com.assignmentservice.assignmentservice.Service;

import com.assignmentservice.assignmentservice.Model.Submission;
import com.assignmentservice.assignmentservice.Repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SubmissionService {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionService.class);

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private FileService fileService;

    @Autowired
    private GradingService gradingService;

    @Transactional
    public Submission saveSubmission(String userId, String assignmentId, String studentName, String studentRollNumber, MultipartFile file) throws IOException {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (assignmentId == null || assignmentId.isEmpty()) {
            throw new IllegalArgumentException("Assignment ID cannot be null or empty");
        }
        if (studentName == null || studentName.isEmpty()) {
            throw new IllegalArgumentException("Student name cannot be null or empty");
        }
        if (studentRollNumber == null || studentRollNumber.isEmpty()) {
            throw new IllegalArgumentException("Student roll number cannot be null or empty");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        // Debug: Verify gradingService injection
        if (gradingService == null) {
            logger.error("GradingService is not injected into SubmissionService");
            throw new IllegalStateException("GradingService is not injected");
        }

        // Generate submissionId
        String submissionId = UUID.randomUUID().toString();

        // Upload the file to GridFS
        String fileName = file.getOriginalFilename();
        String fileNo;
        try {
            fileNo = fileService.uploadSubmissionFile(file, submissionId, assignmentId, fileName);
        } catch (IOException e) {
            logger.error("Failed to upload file for submissionId: {}, assignmentId: {}", submissionId, assignmentId, e);
            throw e;
        }

        Submission submission = new Submission();
        submission.setId(submissionId);
        submission.setUserId(userId);
        submission.setStudentName(studentName);
        submission.setStudentRollNumber(studentRollNumber);
        submission.setAssignmentId(assignmentId);
        submission.setFileNo(fileNo);
        submission.setSubmittedAt(LocalDateTime.now());

        // Save the submission first
        Submission savedSubmission;
        try {
            savedSubmission = submissionRepository.save(submission);
            logger.info("Successfully saved submission for userId: {}, assignmentId: {}", userId, assignmentId);
        } catch (Exception e) {
            logger.error("Failed to save submission for userId: {}, assignmentId: {}", userId, assignmentId, e);
            throw new RuntimeException("Failed to save submission: " + e.getMessage(), e);
        }

        // Auto-generate grading after saving submission
        try {
            logger.info("Attempting to auto-generate grading for userId: {}, assignmentId: {}", userId, assignmentId);
            gradingService.autoGenerateGrading(userId, assignmentId);
            logger.info("Successfully auto-generated grading for userId: {}, assignmentId: {}", userId, assignmentId);
        } catch (Exception e) {
            logger.error("Failed to auto-generate grading for userId: {}, assignmentId: {}", userId, assignmentId, e);
            throw new RuntimeException("Failed to auto-generate grading: " + e.getMessage(), e);
        }

        return savedSubmission;
    }

    @Transactional
    public void deleteSubmissionByAssignmentIdAndUserId(String assignmentId, String userId) {
        try {
            logger.info("Deleting submission for userId: {}, assignmentId: {}", userId, assignmentId);
            Submission submission = submissionRepository.findByAssignmentIdAndUserId(assignmentId, userId)
                    .orElseThrow(() -> new IllegalArgumentException("No submission found for userId: " + userId + ", assignmentId: " + assignmentId));
            if (submission.getFileNo() != null) {
                fileService.deleteFileByFileNo(submission.getFileNo());
            }
            gradingService.deleteGrading(userId, assignmentId);
            submissionRepository.deleteByAssignmentIdAndUserId(assignmentId, userId);
            logger.info("Successfully deleted submission for userId: {}, assignmentId: {}", userId, assignmentId);
        } catch (Exception e) {
            logger.error("Failed to delete submission for userId: {}, assignmentId: {}", userId, assignmentId, e);
            throw new RuntimeException("Failed to delete submission: " + e.getMessage(), e);
        }
    }

    public List<Submission> getAllSubmission() {
        return submissionRepository.findAll();
    }

    public List<Submission> getSubmissionsByAssignmentId(String assignmentId) {
        return submissionRepository.findByAssignmentId(assignmentId);
    }
}