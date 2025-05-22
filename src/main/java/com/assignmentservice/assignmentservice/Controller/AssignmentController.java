package com.assignmentservice.assignmentservice.Controller;

import com.assignmentservice.assignmentservice.Model.Assignment;
import com.assignmentservice.assignmentservice.Service.AssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/assignments")
@CrossOrigin(origins = "http://localhost:3000")
public class AssignmentController {

    @Autowired
    private AssignmentService assignmentService;

    @PostMapping("/createassignment")
    public ResponseEntity<Assignment> createAssignment(@Valid @RequestBody Assignment assignment) {
        Assignment savedAssignment = assignmentService.saveAssignment(assignment);
        return ResponseEntity.ok(savedAssignment);
    }

    @PutMapping("/updateassignment")
    public ResponseEntity<Assignment> updateAssignment(@Valid @RequestBody Assignment assignment) {
        String id = assignment.getAssignmentId();
        if (id == null || id.isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }
        Optional<Assignment> existingAssignment = assignmentService.getAssignmentById(id);
        if (existingAssignment.isPresent()) {
            Assignment updatedAssignment = assignmentService.saveAssignment(assignment);
            return ResponseEntity.ok(updatedAssignment);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/deleteassignment")
    public ResponseEntity<Void> deleteAssignment(@RequestBody AssignmentIdRequest idRequest) {
        String id = idRequest.getAssignmentId();
        if (id == null || id.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Optional<Assignment> existingAssignment = assignmentService.getAssignmentById(id);
        if (existingAssignment.isPresent()) {
            assignmentService.deleteAssignment(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<Assignment>> getAllAssignments() {
        List<Assignment> assignments = assignmentService.getAllAssignments();
        return ResponseEntity.ok(assignments);
    }
}

class AssignmentIdRequest {
    private String assignmentId;

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }
}