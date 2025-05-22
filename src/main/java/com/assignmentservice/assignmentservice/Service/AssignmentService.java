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

    @Autowired
    private FileService fileService;

    public Assignment saveAssignment(Assignment assignment) {
        if (assignment.getAssignmentId() == null || assignment.getAssignmentId().isBlank()) {
            assignment.setAssignmentId(UUID.randomUUID().toString());
        }
        if (assignment.getCreatedAt() == null) {
            assignment.setCreatedAt(LocalDateTime.now());
        }

        return assignmentRepository.save(assignment);
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
        } else {
            throw new ValidationException("Assignment not found with ID: " + id);
        }
    }

    public List<Assignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    public Optional<Assignment> getAssignmentById(String id) {
        return assignmentRepository.findById(id);
    }
}