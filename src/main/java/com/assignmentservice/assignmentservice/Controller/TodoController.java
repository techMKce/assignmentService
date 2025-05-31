package com.assignmentservice.assignmentservice.Controller;

import com.assignmentservice.assignmentservice.Model.Todo;
import com.assignmentservice.assignmentservice.Repository.TodoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mongodb.MongoException;

import java.util.*;

@RestController
@CrossOrigin(origins = "http://localhost:8080")
@RequestMapping("/api/todos")
public class TodoController {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TodoController.class);

    @Autowired
    private TodoRepository todoRepository;

    @GetMapping
    public ResponseEntity<?> getTodosByStudentRollNumber(@RequestParam("studentRollNumber") String studentRollNumber) {
        try {
            String validRollNumber = Optional.ofNullable(studentRollNumber)
                    .filter(id -> !id.isBlank())
                    .orElseThrow(() -> new IllegalArgumentException("Student Roll Number cannot be null or blank"));
            List<Todo> todos = todoRepository.findByStudentRollNumber(validRollNumber);
            return ResponseEntity.ok(Map.of(
                    "message", "Todos retrieved successfully",
                    "todos", todos));
        } catch (IllegalArgumentException e) {
            logger.error("Invalid request for studentRollNumber: {}: {}", studentRollNumber, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
        } catch (MongoException e) {
            logger.error("Database error retrieving todos for studentRollNumber: {}: {}", 
                    studentRollNumber, e.getMessage());
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Database error retrieving todos: " + e.getMessage()));
        }
    }
}