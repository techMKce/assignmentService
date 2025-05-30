package com.assignmentservice.assignmentservice.Service;

import com.assignmentservice.assignmentservice.Model.Assignment;
import com.assignmentservice.assignmentservice.Repository.AssignmentRepository;
import jakarta.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AssignmentService {

    private static final Logger logger = LoggerFactory.getLogger(AssignmentService.class);

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private FileService fileService;

    public Assignment saveAssignment(Assignment assignment) {
        String assignmentId = Optional.ofNullable(assignment.getAssignmentId())
                .filter(id -> !id.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
        assignment.setAssignmentId(assignmentId);

        Optional.ofNullable(assignment.getCreatedAt())
                .orElseGet(() -> {
                    assignment.setCreatedAt(LocalDateTime.now());
                    return assignment.getCreatedAt();
                });

        Optional.ofNullable(assignment.getCourseId())
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new ValidationException("Course ID cannot be null or empty"));
        Optional.ofNullable(assignment.getCourseName())
                .filter(name -> !name.isBlank())
                .orElseThrow(() -> new ValidationException("Course Name cannot be null or empty"));
        Optional.ofNullable(assignment.getCourseFaculty())
                .filter(faculty -> !faculty.isBlank())
                .orElseThrow(() -> new ValidationException("Course Faculty cannot be null or empty"));
        Optional.ofNullable(assignment.getTitle())
                .filter(title -> !title.isBlank())
                .orElseThrow(() -> new ValidationException("Title cannot be null or empty"));
        Optional.ofNullable(assignment.getDescription())
                .filter(desc -> !desc.isBlank())
                .orElseThrow(() -> new ValidationException("Description cannot be null or empty"));
        Optional.ofNullable(assignment.getDueDate())
                .orElseThrow(() -> new ValidationException("Due date cannot be null"));
        Optional.ofNullable(assignment.getFileName())
                .filter(fileName -> !fileName.isBlank())
                .orElseThrow(() -> new ValidationException("File name cannot be null or empty"));

        logger.info("Saving assignment with ID: {}", assignmentId);
        Assignment savedAssignment = assignmentRepository.save(assignment);
        logger.info("Successfully saved assignment with ID: {}", savedAssignment.getAssignmentId());
        return savedAssignment;
    }

    public void deleteAssignment(String id) {
        String validId = Optional.ofNullable(id)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new ValidationException("Assignment ID cannot be null or empty"));
        assignmentRepository.findById(validId)
                .ifPresentOrElse(
                        assignment -> {
                            if (assignment.getFileName() != null) {
                                fileService.deleteFileByFileName(assignment.getFileName());
                            }
                            assignmentRepository.deleteById(validId);
                            logger.info("Successfully deleted assignment with ID: {}", validId);
                        },
                        () -> {
                            logger.warn("No assignment found with ID: {}", validId);
                            throw new ValidationException("Assignment not found with ID: " + validId);
                        }
                );
    }

    public List<Assignment> getAllAssignments() {
        logger.info("Fetching all assignments from the database");
        List<Assignment> assignments = assignmentRepository.findAll();
        logger.info("Retrieved {} assignments", assignments.size());
        return assignments;
    }

    public Optional<Assignment> getAssignmentById(String id) {
        logger.info("Fetching assignment with ID: {}", id);
        return Optional.ofNullable(id)
                .filter(s -> !s.isBlank())
                .map(assignmentRepository::findById)
                .orElseGet(() -> {
                    logger.warn("No assignment found with ID: {}", id);
                    return Optional.empty();
                });
    }

    public List<Assignment> getAssignmentsByCourseId(String courseId) {
        String validCourseId = Optional.ofNullable(courseId)
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new ValidationException("Course ID cannot be null or empty"));
        logger.info("Fetching assignments for course ID: {}", validCourseId);
        List<Assignment> assignments = assignmentRepository.findByCourseId(validCourseId);
        logger.info("Retrieved {} assignments for course ID: {}", assignments.size(), validCourseId);
        return assignments;
    }
}