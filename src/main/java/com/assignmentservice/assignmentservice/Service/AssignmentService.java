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
        if (assignment.getAssignmentId() == null || assignment.getAssignmentId().isBlank()) {
            assignment.setAssignmentId(UUID.randomUUID().toString());
        }
        if (assignment.getCreatedAt() == null) {
            assignment.setCreatedAt(LocalDateTime.now());
        }

        logger.info("Saving assignment with ID: {}", assignment.getAssignmentId());
        Assignment savedAssignment = assignmentRepository.save(assignment);
        logger.info("Successfully saved assignment with ID: {}", savedAssignment.getAssignmentId());
        return savedAssignment;
    }

    public void deleteAssignment(String id) {
        if (id == null || id.isBlank()) {
            throw new ValidationException("Assignment ID cannot be null or empty");
        }
        Optional<Assignment> assignment = assignmentRepository.findById(id);
        if (assignment.isPresent()) {
            String assignmentId = assignment.get().getAssignmentId();
            fileService.deleteFileByAssignmentId(assignmentId);
            assignmentRepository.deleteById(id);
            logger.info("Successfully deleted assignment with ID: {}", id);
        } else {
            throw new ValidationException("Assignment not found with ID: " + id);
        }
    }

    public List<Assignment> getAllAssignments() {
        logger.info("Fetching all assignments from the database");
        List<Assignment> assignments = assignmentRepository.findAll();
        logger.info("Retrieved {} assignments", assignments.size());
        return assignments;
    }

    public Optional<Assignment> getAssignmentById(String id) {
        logger.info("Fetching assignment with ID: {}", id);
        Optional<Assignment> assignment = assignmentRepository.findById(id);
        if (assignment.isPresent()) {
            logger.info("Found assignment with ID: {}", id);
        } else {
            logger.warn("No assignment found with ID: {}", id);
        }
        return assignment;
    }
}