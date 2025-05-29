package com.assignmentservice.assignmentservice.Service;

import com.assignmentservice.assignmentservice.Model.Submission;
import com.assignmentservice.assignmentservice.Model.Todo;
import com.assignmentservice.assignmentservice.Repository.SubmissionRepository;
import com.assignmentservice.assignmentservice.Repository.TodoRepository;
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

    @Autowired
    private TodoRepository todoRepository;

    @Transactional
    public Submission saveSubmission(String assignmentId, String studentName, String studentRollNumber,
            MultipartFile file) throws IOException {
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

        if (gradingService == null) {
            logger.error("GradingService is not injected into SubmissionService");
            throw new IllegalStateException("GradingService is not injected");
        }

        String submissionId = UUID.randomUUID().toString();
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
        submission.setStudentName(studentName);
        submission.setStudentRollNumber(studentRollNumber);
        submission.setAssignmentId(assignmentId);
        submission.setFileNo(fileNo);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus("Accepted");

        Submission savedSubmission;
        try {
            savedSubmission = submissionRepository.save(submission);
            logger.info("Successfully saved submission for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId);
        } catch (Exception e) {
            logger.error("Failed to save submission for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId, e);
            throw new RuntimeException("Failed to save submission: " + e.getMessage(), e);
        }

        try {
            logger.info("Attempting to auto-generate grading for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId);
            gradingService.autoGenerateGrading(studentRollNumber, assignmentId);
            logger.info("Successfully auto-generated grading for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId);
        } catch (Exception e) {
            logger.error("Failed to auto-generate grading for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId, e);
            throw new RuntimeException("Failed to auto-generate grading: " + e.getMessage(), e);
        }

        return savedSubmission;
    }

    @Transactional
    public Submission updateSubmissionStatus(String submissionId, String status, String assignmentTitle) {
        if (submissionId == null || submissionId.isEmpty()) {
            throw new IllegalArgumentException("Submission ID cannot be null or empty");
        }
        if (status == null || (!status.equals("Accepted") && !status.equals("Rejected"))) {
            throw new IllegalArgumentException("Status must be either 'Accepted' or 'Rejected'");
        }

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found for ID: " + submissionId));

        submission.setStatus(status);

        try {
            Submission updatedSubmission = submissionRepository.save(submission);
            logger.info("Successfully updated submission status to {} for submissionId: {}", status, submissionId);

            if (status.equals("Rejected")) {
                Todo todo = new Todo();
                todo.setStudentRollNumber(submission.getStudentRollNumber());
                todo.setAssignmentId(submission.getAssignmentId());
                todo.setAssignmentTitle(assignmentTitle);
                todo.setStatus("Pending");

                todoRepository.save(todo);
                logger.info("Created todo for rejected submission: studentRollNumber={}, assignmentId={}", 
                    submission.getStudentRollNumber(), submission.getAssignmentId());
            }

            return updatedSubmission;
        } catch (Exception e) {
            logger.error("Failed to update submission status for submissionId: {}", submissionId, e);
            throw new RuntimeException("Failed to update submission status: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteSubmissionByAssignmentIdAndStudentRollNumber(String assignmentId, String studentRollNumber) {
        try {
            logger.info("Deleting submission for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId);
            Submission submission = submissionRepository
                    .findByAssignmentIdAndStudentRollNumber(assignmentId, studentRollNumber)
                    .orElseThrow(() -> new IllegalArgumentException("No submission found for studentRollNumber: "
                            + studentRollNumber + ", assignmentId: " + assignmentId));
            if (submission.getFileNo() != null) {
                fileService.deleteFileByFileNo(submission.getFileNo());
            }
            gradingService.deleteGrading(studentRollNumber, assignmentId);
            submissionRepository.deleteByAssignmentIdAndStudentRollNumber(assignmentId, studentRollNumber);
            logger.info("Successfully deleted submission for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId);
        } catch (Exception e) {
            logger.error("Failed to delete submission for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId, e);
            throw new RuntimeException("Failed to delete submission: " + e.getMessage(), e);
        }
    }

    public Submission getSubmissionById(String submissionId) {
        return submissionRepository.findById(submissionId).orElse(null);
    }

    public List<Submission> getSubmissionsByAssignmentId(String assignmentId) {
        return submissionRepository.findByAssignmentId(assignmentId);
    }

    public long countByStudentRollNumberAndAssignmentIds(String studentRollNumber, List<String> assignmentIds) {
        logger.info("Counting submissions for studentRollNumber: {}, assignmentIds: {}", 
            studentRollNumber, assignmentIds);
        long count = submissionRepository.countByStudentRollNumberAndAssignmentIdIn(studentRollNumber, assignmentIds);
        logger.info("Found {} submissions", count);
        return count;
    }
}