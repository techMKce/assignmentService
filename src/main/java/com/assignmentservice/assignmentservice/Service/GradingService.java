package com.assignmentservice.assignmentservice.Service;

import com.assignmentservice.assignmentservice.Model.Grading;
import com.assignmentservice.assignmentservice.Model.Submission;
import com.assignmentservice.assignmentservice.Model.Assignment;
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
import java.util.stream.IntStream;

@Service
public class GradingService {

    private static final Logger logger = LoggerFactory.getLogger(GradingService.class);

    @Autowired
    private GradingRepository gradingRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private AssignmentService assignmentService;

    @Transactional
    public Grading autoGenerateGrading(String studentRollNumber, String assignmentId) {
        String validRollNumber = Optional.ofNullable(studentRollNumber)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Student Roll Number cannot be null or empty"));
        String validAssignmentId = Optional.ofNullable(assignmentId)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Assignment ID cannot be null or empty"));

        return gradingRepository.findByStudentRollNumberAndAssignmentId(validRollNumber, validAssignmentId)
                .orElseGet(() -> {
                    logger.info(
                            "No existing grading entry for studentRollNumber: {}, assignmentId: {}. Auto-generating.",
                            validRollNumber, validAssignmentId);
                    Submission submission = submissionRepository
                            .findByAssignmentIdAndStudentRollNumber(validAssignmentId, validRollNumber)
                            .orElseThrow(() -> {
                                logger.error("No submission found for studentRollNumber: {}, assignmentId: {}",
                                        validRollNumber, validAssignmentId);
                                return new IllegalStateException("Cannot auto-generate grading: No submission found");
                            });

                    Grading grading = new Grading();
                    grading.setAssignmentId(validAssignmentId);
                    grading.setStudentName(submission.getStudentName());
                    grading.setStudentRollNumber(validRollNumber);
                    grading.setGrade(null);
                    grading.setFeedback(null);
                    grading.setGradedAt(LocalDateTime.now());

                    try {
                        return gradingRepository.save(grading);
                    } catch (Exception e) {
                        logger.error("Failed to auto-generate grading: {}", e.getMessage());
                        throw new RuntimeException("Failed to auto-generate grading", e);
                    }
                });
    }

    @Transactional
    public Grading assignGrade(String studentRollNumber, String assignmentId, String grade, String feedback) {
        String validRollNumber = Optional.ofNullable(studentRollNumber)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Student Roll Number cannot be null or empty"));
        String validAssignmentId = Optional.ofNullable(assignmentId)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Assignment ID cannot be null or empty"));
        String validGrade = Optional.ofNullable(grade)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Grade is required"));

        Grading grading = gradingRepository.findByStudentRollNumberAndAssignmentId(validRollNumber, validAssignmentId)
                .orElseGet(() -> {
                    logger.info("No existing grading for studentRollNumber: {}, assignmentId: {}. Creating new.",
                            validRollNumber, validAssignmentId);
                    Submission submission = submissionRepository
                            .findByAssignmentIdAndStudentRollNumber(validAssignmentId, validRollNumber)
                            .orElseThrow(() -> {
                                logger.error("No submission found for studentRollNumber: {}, assignmentId: {}",
                                        validRollNumber, validAssignmentId);
                                return new IllegalStateException("Cannot create grading: No submission found");
                            });
                    Grading newGrading = new Grading();
                    newGrading.setStudentRollNumber(validRollNumber);
                    newGrading.setAssignmentId(validAssignmentId);
                    newGrading.setStudentName(submission.getStudentName());
                    return newGrading;
                });

        grading.setGrade(validGrade);
        grading.setFeedback(feedback);
        grading.setGradedAt(LocalDateTime.now());

        try {
            return gradingRepository.save(grading);
        } catch (Exception e) {
            logger.error("Failed to save grading: {}", e.getMessage());
            throw new RuntimeException("Failed to save grading", e);
        }
    }

    @Transactional
    public Grading deleteAssignedGrade(String studentRollNumber, String assignmentId) {
        String validRollNumber = Optional.ofNullable(studentRollNumber)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Student Roll Number cannot be null or empty"));
        String validAssignmentId = Optional.ofNullable(assignmentId)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Assignment ID cannot be null or empty"));

        Grading grading = gradingRepository.findByStudentRollNumberAndAssignmentId(validRollNumber, validAssignmentId)
                .orElseThrow(() -> {
                    logger.warn("No grading entry found for studentRollNumber: {}, assignmentId: {}", validRollNumber,
                            validAssignmentId);
                    return new IllegalArgumentException("No grading entry found");
                });

        grading.setGrade(null);
        grading.setFeedback(null);
        grading.setGradedAt(LocalDateTime.now());

        try {
            return gradingRepository.save(grading);
        } catch (Exception e) {
            logger.error("Failed to delete grading: {}", e.getMessage());
            throw new RuntimeException("Failed to delete grading", e);
        }
    }

    public List<Grading> getGradingsByAssignmentId(String assignmentId) {
        return Optional.ofNullable(assignmentId)
                .filter(s -> !s.isBlank())
                .map(gradingRepository::findByAssignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment ID is required"));
    }

    @Transactional
    public void deleteGrading(String studentRollNumber, String assignmentId) {
        Optional.ofNullable(studentRollNumber)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Student Roll Number cannot be null or empty"));
        Optional.ofNullable(assignmentId)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Assignment ID cannot be null or empty"));

        gradingRepository.deleteByStudentRollNumberAndAssignmentId(studentRollNumber, assignmentId);
    }

    public String generateGradesCsv() throws IOException {
        List<Grading> gradings = gradingRepository.findAll();

        StringWriter stringWriter = new StringWriter();
        try (CSVWriter csvWriter = new CSVWriter(stringWriter)) {
            csvWriter.writeNext(new String[] { "Assignment ID", "Student Name", "Student Roll Number", "Grade",
                    "Feedback", "Graded At" });
            gradings.forEach(grading -> csvWriter.writeNext(new String[] {
                    Optional.ofNullable(grading.getAssignmentId()).orElse(""),
                    Optional.ofNullable(grading.getStudentName()).orElse(""),
                    Optional.ofNullable(grading.getStudentRollNumber()).orElse(""),
                    Optional.ofNullable(grading.getGrade()).orElse(""),
                    Optional.ofNullable(grading.getFeedback()).orElse(""),
                    Optional.ofNullable(grading.getGradedAt()).map(LocalDateTime::toString).orElse("")
            }));
        }
        return stringWriter.toString();
    }

    public String generateGradingCsvForAssignment(String assignmentId) throws IOException {
        logger.info("Generating grading CSV for assignmentId: {}", assignmentId);
        String validAssignmentId = Optional.ofNullable(assignmentId)
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Assignment ID cannot be null or empty"));

        // Fetch Assignment for Course Name and Title
        Assignment assignment = assignmentService.getAssignmentById(validAssignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found for ID: " + validAssignmentId));
        String courseName = assignment.getCourseName();
        String assignmentTitle = assignment.getTitle();

        List<Grading> gradings = gradingRepository.findByAssignmentId(validAssignmentId);
        if (gradings.isEmpty()) {
            throw new IllegalArgumentException("No grading data found for assignment ID: " + validAssignmentId);
        }

        StringWriter writer = new StringWriter();
        writer.write(
                "S.No,Student Name,Student Roll Number,Student Department,Course Name,Assignment Title,Grade\n");
        IntStream.range(0, gradings.size()).forEach(i -> {
            Grading grading = gradings.get(i);
            writer.write(String.format("%d,%s,%s,%s,%s,%s,%s\n",
                    i + 1,
                    escapeCsv(Optional.ofNullable(grading.getStudentName()).orElse("Unknown")),
                    escapeCsv(Optional.ofNullable(grading.getStudentRollNumber()).orElse("Unknown")),
                    escapeCsv(Optional.ofNullable(grading.getStudentDepartment()).orElse("Unknown")),
                    escapeCsv(Optional.ofNullable(courseName).orElse("Unknown")),
                    escapeCsv(Optional.ofNullable(assignmentTitle).orElse("Unknown")),
                    escapeCsv(Optional.ofNullable(grading.getGrade()).orElse("Not Graded"))));

        });

        logger.info("Generated CSV for {} gradings for assignmentId: {}", gradings.size(), validAssignmentId);
        return writer.toString();
    }

    private String escapeCsv(String value) {
        return Optional.ofNullable(value)
                .map(v -> v.contains(",") || v.contains("\"") || v.contains("\n")
                        ? "\"" + v.replace("\"", "\"\"") + "\""
                        : v)
                .orElse("");
    }
}