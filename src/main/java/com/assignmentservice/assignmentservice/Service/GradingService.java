package com.assignmentservice.assignmentservice.Service;

import com.assignmentservice.assignmentservice.Model.Grading;
import com.assignmentservice.assignmentservice.Repository.GradingRepository;
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

    
    public Grading autoGenerateGrading(String userId, String assignmentId) {
        Optional<Grading> existingGrading = findExistingGrading(userId, assignmentId);
        if (existingGrading.isPresent()) {
            logger.warn("Grading entry already exists for userId: {}, assignmentId: {}. Skipping auto-generation.", userId, assignmentId);
            return existingGrading.get();
        }

        Grading grading = new Grading();
        grading.setUserId(userId);
        grading.setAssignmentId(assignmentId);
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
            grading = new Grading();
            grading.setUserId(userId);
            grading.setAssignmentId(assignmentId);
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
            csvWriter.writeNext(new String[]{"User ID", "Assignment ID", "Grade", "Feedback", "Graded At"});
            for (Grading grading : gradings) {
                csvWriter.writeNext(new String[]{
                    grading.getUserId(),
                    grading.getAssignmentId(),
                    grading.getGrade() != null ? grading.getGrade() : "",
                    grading.getFeedback() != null ? grading.getFeedback() : "",
                    grading.getGradedAt() != null ? grading.getGradedAt().toString() : ""
                });
            }
        }
        return stringWriter.toString();
    }
}
