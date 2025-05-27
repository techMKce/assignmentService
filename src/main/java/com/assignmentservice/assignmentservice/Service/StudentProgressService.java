package com.assignmentservice.assignmentservice.Service;

import com.assignmentservice.assignmentservice.Model.Assignment;
import com.assignmentservice.assignmentservice.Model.StudentProgress;
import com.assignmentservice.assignmentservice.Model.Grading;
import com.assignmentservice.assignmentservice.Repository.AssignmentRepository;
import com.assignmentservice.assignmentservice.Repository.GradingRepository;
import com.assignmentservice.assignmentservice.Repository.StudentProgressRepository;
import com.assignmentservice.assignmentservice.Repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class StudentProgressService {

    private static final Logger logger = LoggerFactory.getLogger(StudentProgressService.class);

    @Autowired
    private StudentProgressRepository studentProgressRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private GradingRepository gradingRepository;

    @Transactional
    public StudentProgress updateProgressAndGrade(String studentRollNumber, String courseId) {
        if (studentRollNumber == null || studentRollNumber.isBlank() || courseId == null || courseId.isBlank()) {
            logger.error("Invalid input: studentRollNumber or courseId is null or blank");
            throw new IllegalArgumentException("Student Roll Number and Course ID cannot be null or empty");
        }

        logger.info("Updating progress and grade for studentRollNumber: {}, courseId: {}", studentRollNumber, courseId);

        // Calculate progress
        double progressPercentage = calculateProgress(studentRollNumber, courseId);
        // Calculate average grade
        Double averageGrade = calculateAverageGrade(studentRollNumber, courseId);

        // Find or create StudentProgress entry
        Optional<StudentProgress> existingProgress = studentProgressRepository.findByStudentRollNumberAndCourseId(studentRollNumber, courseId);
        StudentProgress progress = existingProgress.orElse(new StudentProgress());
        progress.setStudentRollNumber(studentRollNumber);
        progress.setCourseId(courseId);
        progress.setProgressPercentage(progressPercentage);
        progress.setAverageGrade(averageGrade);
        progress.setUpdatedAt(LocalDateTime.now());
        if (progress.getId() == null) {
            progress.setId(UUID.randomUUID().toString());
        }

        StudentProgress savedProgress = studentProgressRepository.save(progress);
        logger.info("Successfully updated progress {}% and average grade {} for studentRollNumber: {}, courseId: {}", 
            progressPercentage, averageGrade != null ? averageGrade : "Not graded", studentRollNumber, courseId);
        return savedProgress;
    }

    public double calculateProgress(String studentRollNumber, String courseId) {
        logger.info("Calculating progress for studentRollNumber: {}, courseId: {}", studentRollNumber, courseId);

        // Count total assignments for the course
        List<Assignment> assignments = assignmentRepository.findByCourseId(courseId);
        long totalAssignments = assignments.size();
        if (totalAssignments == 0) {
            logger.warn("No assignments found for courseId: {}", courseId);
            return 0.0;
        }

        // Extract assignment IDs
        List<String> assignmentIds = assignments.stream()
                .map(Assignment::getAssignmentId)
                .collect(Collectors.toList());
        logger.debug("Retrieved {} assignment IDs for courseId: {}", assignmentIds.size(), courseId);

        if (assignmentIds.isEmpty()) {
            logger.warn("No assignment IDs extracted for courseId: {}", courseId);
            return 0.0;
        }

        // Count submissions for the student in the course
        long submissionCount = submissionRepository.countByStudentRollNumberAndCourseId(studentRollNumber, assignmentIds);
        logger.info("Found {} assignments and {} submissions for studentRollNumber: {}, courseId: {}", 
            totalAssignments, submissionCount, studentRollNumber, courseId);

        double progressPercentage = (double) submissionCount / totalAssignments * 100;
        return Math.round(progressPercentage * 100.0) / 100.0; // Round to 2 decimal places
    }

    public Double calculateAverageGrade(String studentRollNumber, String courseId) {
        logger.info("Calculating average grade for studentRollNumber: {}, courseId: {}", studentRollNumber, courseId);

        List<Assignment> assignments = assignmentRepository.findByCourseId(courseId);
        if (assignments.isEmpty()) {
            logger.warn("No assignments found for courseId: {}", courseId);
            return null;
        }

        double totalGrade = 0.0;
        int gradedAssignments = 0;
        for (Assignment assignment : assignments) {
            String assignmentId = assignment.getAssignmentId();
            Optional<Grading> gradingOpt = gradingRepository.findByStudentRollNumberAndAssignmentId(studentRollNumber, assignmentId);
            if (gradingOpt.isPresent()) {
                Grading grading = gradingOpt.get();
                String grade = grading.getGrade();
                if (grade != null && !grade.isBlank()) {
                    totalGrade += convertLetterGradeToNumber(grade);
                    gradedAssignments++;
                }
            }
        }

        if (gradedAssignments == 0) {
            logger.info("No grades found for studentRollNumber: {}, courseId: {}", studentRollNumber, courseId);
            return null;
        }

        double averageGrade = totalGrade / gradedAssignments;
        logger.info("Calculated average grade {} for studentRollNumber: {}, courseId: {}", averageGrade, studentRollNumber, courseId);
        return Math.round(averageGrade * 100.0) / 100.0; 
    }

    private double convertLetterGradeToNumber(String grade) {
        switch (grade.toUpperCase()) {
            case "O":
                return 100.0;
            case "A+":
                return 95.0;
            case "A":
                return 90.0;
            case "B+":
                return 85.0;
            case "B":
                return 80.0;
            case "C+":
                return 75.0;
            case "C":
                return 70.0;
            case "D+":
                return 65.0;
            case "D":
                return 60.0;
            case "F":
                return 0.0;
            default:
                logger.error("Invalid grade format: {}", grade);
                throw new IllegalArgumentException("Invalid grade format: " + grade);
        }
    }

    public Optional<StudentProgress> getProgressByStudentAndCourse(String studentRollNumber, String courseId) {
        logger.info("Fetching progress for studentRollNumber: {}, courseId: {}", studentRollNumber, courseId);
        return studentProgressRepository.findByStudentRollNumberAndCourseId(studentRollNumber, courseId);
    }
}