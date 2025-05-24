package com.assignmentservice.assignmentservice.Service;

import com.assignmentservice.assignmentservice.Model.Grading;
import com.assignmentservice.assignmentservice.Model.Submission;
import com.assignmentservice.assignmentservice.Repository.GradingRepository;
import com.assignmentservice.assignmentservice.Repository.SubmissionRepository;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class GradingService {

    private static final Logger logger = LoggerFactory.getLogger(GradingService.class);

    @Autowired
    private GradingRepository gradingRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    public Grading autoGenerateGrading(String userId, String assignmentId) {
        Optional<Grading> existingGrading = findExistingGrading(userId, assignmentId);
        if (existingGrading.isPresent()) {
            logger.warn("Grading entry already exists for userId: {}, assignmentId: {}. Skipping auto-generation.", userId, assignmentId);
            return existingGrading.get();
        }

        List<Submission> submissions = submissionRepository.findByAssignmentIdAndUserId(assignmentId, userId);
        if (submissions.isEmpty()) {
            logger.error("No submission found for userId: {}, assignmentId: {}", userId, assignmentId);
            throw new IllegalStateException("Cannot auto-generate grading: No submission found for userId: " + userId + ", assignmentId: " + assignmentId);
        }
        Submission submission = submissions.get(0);
        String studentName = submission.getStudentName();
        String studentRollNumber = submission.getStudentRollNumber();

        Grading grading = new Grading();
        grading.setUserId(userId);
        grading.setAssignmentId(assignmentId);
        grading.setStudentName(studentName);
        grading.setStudentRollNumber(studentRollNumber);
        grading.setGrade(null);
        grading.setFeedback(null);
        grading.setGradedAt(LocalDateTime.now());

        try {
            Grading savedGrading = gradingRepository.save(grading);
            logger.info("Successfully auto-generated grading for userId: {}, assignmentId: {}", userId, assignmentId);
            return savedGrading;
        } catch (Exception e) {
            logger.error("Failed to auto-generate grading for userId: {}, assignmentId: {}", userId, assignmentId, e);
            throw new RuntimeException("Failed to auto-generate grading: " + e.getMessage(), e);
        }
    }

    private Optional<Grading> findExistingGrading(String userId, String assignmentId) {
        Optional<Grading> grading = gradingRepository.findByUserIdAndAssignmentId(userId, assignmentId);
        if (grading.isPresent()) {
            return grading;
        }

        String duplicatedUserId = userId + "," + userId;
        String duplicatedAssignmentId = assignmentId + "," + assignmentId;
        grading = gradingRepository.findByUserIdAndAssignmentId(duplicatedUserId, duplicatedAssignmentId);
        if (grading.isPresent()) {
            return grading;
        }

        grading = gradingRepository.findByUserIdAndAssignmentId(userId, duplicatedAssignmentId);
        if (grading.isPresent()) {
            return grading;
        }
        grading = gradingRepository.findByUserIdAndAssignmentId(duplicatedUserId, assignmentId);
        return grading;
    }

    public Grading assignGrade(String userId, String assignmentId, String grade, String feedback) {
        if (userId == null || userId.isEmpty()) {  
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (assignmentId == null || assignmentId.isEmpty()) {
            throw new IllegalArgumentException("Assignment ID cannot be null or empty");
        }

        Optional<Grading> existingGrading = findExistingGrading(userId, assignmentId);
        Grading grading;

        if (existingGrading.isPresent()) {
            logger.info("Found existing grading entry for userId: {}, assignmentId: {}. Updating.", userId, assignmentId);
            grading = existingGrading.get();
            grading.setUserId(userId);
            grading.setAssignmentId(assignmentId);
        } else {
            logger.info("No existing grading entry found for userId: {}, assignmentId: {}. Creating new.", userId, assignmentId);
            List<Submission> submissions = submissionRepository.findByAssignmentIdAndUserId(assignmentId, userId);
            if (submissions.isEmpty()) {
                logger.error("No submission found for userId: {}, assignmentId: {}", userId, assignmentId);
                throw new IllegalStateException("Cannot create grading: No submission found for userId: " + userId + ", assignmentId: " + assignmentId);
            }
            Submission submission = submissions.get(0);
            grading = new Grading();
            grading.setUserId(userId);
            grading.setAssignmentId(assignmentId);
            grading.setStudentName(submission.getStudentName());
            grading.setStudentRollNumber(submission.getStudentRollNumber());
        }

        if (grade == null || grade.isEmpty()) {
            throw new IllegalArgumentException("Grade is required");
        }

        grading.setGrade(grade);
        grading.setFeedback(feedback);
        grading.setGradedAt(LocalDateTime.now());

        try {
            Grading savedGrading = gradingRepository.save(grading);
            logger.info("Successfully saved grading for userId: {}, assignmentId: {}", userId, assignmentId);
            return savedGrading;
        } catch (Exception e) {
            logger.error("Failed to save grading for userId: {}, assignmentId: {}", userId, assignmentId, e);
            throw new RuntimeException("Failed to save grading: " + e.getMessage(), e);
        }
    }

    public Grading deleteAssignedGrade(String userId, String assignmentId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (assignmentId == null || assignmentId.isEmpty()) {
            throw new IllegalArgumentException("Assignment ID cannot be null or empty");
        }

        Optional<Grading> existingGrading = findExistingGrading(userId, assignmentId);
        if (!existingGrading.isPresent()) {
            logger.warn("No grading entry found for userId: {}, assignmentId: {}", userId, assignmentId);
            throw new IllegalArgumentException("No grading entry found for the given user and assignment");
        }

        Grading grading = existingGrading.get();
        grading.setGrade(null);
        grading.setFeedback(null);
        grading.setGradedAt(LocalDateTime.now());

        try {
            Grading savedGrading = gradingRepository.save(grading);
            logger.info("Successfully deleted (reset) grading for userId: {}, assignmentId: {}", userId, assignmentId);
            return savedGrading;
        } catch (Exception e) {
            logger.error("Failed to delete (reset) grading for userId: {}, assignmentId: {}", userId, assignmentId, e);
            throw new RuntimeException("Failed to delete grading: " + e.getMessage(), e);
        }
    }

    public List<Grading> getGradingsByAssignmentId(String assignmentId) {
        if (assignmentId == null || assignmentId.isEmpty()) {
            throw new IllegalArgumentException("Assignment ID is required");
        }
        return gradingRepository.findByAssignmentId(assignmentId);
    }

    public void deleteGrading(String userId, String assignmentId) {
        if (userId == null || userId.isEmpty() || assignmentId == null || assignmentId.isEmpty()) {
            throw new IllegalArgumentException("User ID and Assignment ID are required");
        }
        gradingRepository.deleteByUserIdAndAssignmentId(userId, assignmentId);
    }

    public String generateGradesCsv() throws IOException {
        List<Grading> gradings = gradingRepository.findAll();

        StringWriter stringWriter = new StringWriter();
        try (CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            csvWriter.writeNext(new String[]{"User ID", "Assignment ID", "Student Name", "Student Roll Number", "Grade", "Feedback", "Graded At"});
            for (Grading grading : gradings) {
                csvWriter.writeNext(new String[]{
                    grading.getUserId(),
                    grading.getAssignmentId(),
                    grading.getStudentName() != null ? grading.getStudentName() : "",
                    grading.getStudentRollNumber() != null ? grading.getStudentRollNumber() : "",
                    grading.getGrade() != null ? grading.getGrade() : "",
                    grading.getFeedback() != null ? grading.getFeedback() : "",
                    grading.getGradedAt() != null ? grading.getGradedAt().toString() : ""
                });
            }
        }
        return stringWriter.toString();
    }

    public String generateGradesCsvForAssignment(String assignmentId) throws IOException {
        List<Grading> gradings = getGradingsByAssignmentId(assignmentId);
        if (gradings.isEmpty()) {
            throw new IllegalArgumentException("No gradings found for assignment ID: " + assignmentId);
        }

        StringWriter writer = new StringWriter();
        writer.write("S.No,Name,RollNumber,Grade\n");

        int serialNumber = 1;
        for (Grading grading : gradings) {
            String studentName = grading.getStudentName() != null ? grading.getStudentName() : "Unknown";
            String rollNumber = grading.getStudentRollNumber() != null ? grading.getStudentRollNumber() : "Unknown";
            String grade = grading.getGrade() != null ? grading.getGrade() : "Not Graded";

            writer.write(String.format("%d,%s,%s,%s\n",
                    serialNumber++,
                    escapeCsv(studentName),
                    escapeCsv(rollNumber),
                    escapeCsv(grade)));
        }

        if (serialNumber == 1) {
            throw new IllegalArgumentException("No gradings found for assignment ID: " + assignmentId);
        }

        return writer.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}