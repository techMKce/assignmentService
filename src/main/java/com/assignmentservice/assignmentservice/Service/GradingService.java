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
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Grading autoGenerateGrading(String studentRollNumber, String assignmentId) {
        if (studentRollNumber == null || assignmentId == null) {
            logger.error("StudentRollNumber or AssignmentId is null");
            throw new IllegalArgumentException("Student Roll Number and Assignment ID cannot be null");
        }

        Optional<Grading> existingGrading = gradingRepository.findByStudentRollNumberAndAssignmentId(studentRollNumber, assignmentId);
        if (existingGrading.isPresent()) {
            logger.warn("Grading entry already exists for studentRollNumber: {}, assignmentId: {}. Skipping auto-generation.", studentRollNumber, assignmentId);
            return existingGrading.get();
        }

        Optional<Submission> submissionOpt = submissionRepository.findByAssignmentIdAndStudentRollNumber(assignmentId, studentRollNumber);
        if (submissionOpt.isEmpty()) {
            logger.error("No submission found for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId);
            throw new IllegalStateException("Cannot auto-generate grading: No submission found for studentRollNumber: " + studentRollNumber + ", assignmentId: " + assignmentId);
        }
        Submission submission = submissionOpt.get();
        String studentName = submission.getStudentName();

        Grading grading = new Grading();
        grading.setAssignmentId(assignmentId);
        grading.setStudentName(studentName);
        grading.setStudentRollNumber(studentRollNumber);
        grading.setGrade(null);
        grading.setFeedback(null);
        grading.setGradedAt(LocalDateTime.now());

        try {
            Grading savedGrading = gradingRepository.save(grading);
            logger.info("Successfully auto-generated grading for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId);
            return savedGrading;
        } catch (Exception e) {
            logger.error("Failed to auto-generate grading for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId, e);
            throw new RuntimeException("Failed to auto-generate grading: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Grading assignGrade(String studentRollNumber, String assignmentId, String grade, String feedback) {
        if (studentRollNumber == null || studentRollNumber.isEmpty()) {
            throw new IllegalArgumentException("Student Roll Number cannot be null or empty");
        }
        if (assignmentId == null || assignmentId.isEmpty()) {
            throw new IllegalArgumentException("Assignment ID cannot be null or empty");
        }

        Optional<Grading> existingGrading = gradingRepository.findByStudentRollNumberAndAssignmentId(studentRollNumber, assignmentId);
        Grading grading;

        if (existingGrading.isPresent()) {
            logger.info("Found existing grading entry for studentRollNumber: {}, assignmentId: {}. Updating.", studentRollNumber, assignmentId);
            grading = existingGrading.get();
        } else {
            logger.info("No existing grading entry found for studentRollNumber: {}, assignmentId: {}. Creating new.", studentRollNumber, assignmentId);
            Optional<Submission> submissionOpt = submissionRepository.findByAssignmentIdAndStudentRollNumber(assignmentId, studentRollNumber);
            if (submissionOpt.isEmpty()) {
                logger.error("No submission found for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId);
                throw new IllegalStateException("Cannot create grading: No submission found for studentRollNumber: " + studentRollNumber + ", assignmentId: " + assignmentId);
            }
            Submission submission = submissionOpt.get();
            grading = new Grading();
            grading.setStudentRollNumber(studentRollNumber);
            grading.setAssignmentId(assignmentId);
            grading.setStudentName(submission.getStudentName());
        }

        if (grade == null || grade.isEmpty()) {
            throw new IllegalArgumentException("Grade is required");
        }

        grading.setGrade(grade);
        grading.setFeedback(feedback);
        grading.setGradedAt(LocalDateTime.now());

        try {
            Grading savedGrading = gradingRepository.save(grading);
            logger.info("Successfully saved grading for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId);
            return savedGrading;
        } catch (Exception e) {
            logger.error("Failed to save grading for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId, e);
            throw new RuntimeException("Failed to save grading: " + e.getMessage(), e);
        }
    }

    @Transactional
    public Grading deleteAssignedGrade(String studentRollNumber, String assignmentId) {
        if (studentRollNumber == null || studentRollNumber.isEmpty()) {
            throw new IllegalArgumentException("Student Roll Number cannot be null or empty");
        }
        if (assignmentId == null || assignmentId.isEmpty()) {
            throw new IllegalArgumentException("Assignment ID cannot be null or empty");
        }

        Optional<Grading> existingGrading = gradingRepository.findByStudentRollNumberAndAssignmentId(studentRollNumber, assignmentId);
        if (!existingGrading.isPresent()) {
            logger.warn("No grading entry found for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId);
            throw new IllegalArgumentException("No grading entry found for the given student and assignment");
        }

        Grading grading = existingGrading.get();
        grading.setGrade(null);
        grading.setFeedback(null);
        grading.setGradedAt(LocalDateTime.now());

        try {
            Grading savedGrading = gradingRepository.save(grading);
            logger.info("Successfully deleted (reset) grading for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId);
            return savedGrading;
        } catch (Exception e) {
            logger.error("Failed to delete (reset) grading for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId, e);
            throw new RuntimeException("Failed to delete grading: " + e.getMessage(), e);
        }
    }

    public List<Grading> getGradingsByAssignmentId(String assignmentId) {
        if (assignmentId == null || assignmentId.isEmpty()) {
            throw new IllegalArgumentException("Assignment ID is required");
        }
        return gradingRepository.findByAssignmentId(assignmentId);
    }

    @Transactional
    public void deleteGrading(String studentRollNumber, String assignmentId) {
        if (studentRollNumber == null || studentRollNumber.isEmpty() || assignmentId == null || assignmentId.isEmpty()) {
            throw new IllegalArgumentException("Student Roll Number and Assignment ID are required");
        }
        gradingRepository.deleteByStudentRollNumberAndAssignmentId(studentRollNumber, assignmentId);
    }

    public String generateGradesCsv() throws IOException {
        List<Grading> gradings = gradingRepository.findAll();

        StringWriter stringWriter = new StringWriter();
        try (CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            csvWriter.writeNext(new String[]{"Assignment ID", "Student Name", "Student Roll Number", "Grade", "Feedback", "Graded At"});
            for (Grading grading : gradings) {
                csvWriter.writeNext(new String[]{
                        grading.getAssignmentId() != null ? grading.getAssignmentId() : "",
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