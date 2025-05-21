package com.assignmentservice.assignmentservice.Service;

import com.assignmentservice.assignmentservice.Model.Assignment;
import com.assignmentservice.assignmentservice.Repository.AssignmentRepository;

import jakarta.validation.ValidationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AssignmentService {
    @Autowired
    private AssignmentRepository assignmentRepository;

    // Create or Update Assignment
    public Assignment saveAssignment(Assignment assignment) {
        // Validate assignmentId
        if (assignment.getAssignmentId() == null || assignment.getAssignmentId().isBlank()) {
            throw new ValidationException("Assignment ID cannot be null or empty");
        }

        // Set createdAt if null (for creation)
        if (assignment.getCreatedAt() == null) {
            assignment.setCreatedAt(LocalDateTime.now());
        }
        
        return assignmentRepository.save(assignment);
    }

    // Get all Assignments
    public List<Assignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    // Get Assignment by ID
    public Optional<Assignment> getAssignmentById(String id) {
        return assignmentRepository.findById(id);
    }

    // Delete Assignment
    public void deleteAssignment(String id) {
        if (id == null || id.isBlank()) {
            throw new ValidationException("Assignment ID cannot be null or empty");
        }
        assignmentRepository.deleteById(id);
    }
   
}
