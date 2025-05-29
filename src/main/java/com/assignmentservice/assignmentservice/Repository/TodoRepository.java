package com.assignmentservice.assignmentservice.Repository;

import com.assignmentservice.assignmentservice.Model.Todo;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;

public interface TodoRepository extends MongoRepository<Todo, String> {
      Optional<Todo> findByStudentRollNumberAndAssignmentId(String studentRollNumber, String assignmentId);
    void deleteByStudentRollNumberAndAssignmentId(String studentRollNumber, String assignmentId);
}