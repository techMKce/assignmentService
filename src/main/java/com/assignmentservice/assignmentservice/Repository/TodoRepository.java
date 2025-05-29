package com.assignmentservice.assignmentservice.Repository;

import com.assignmentservice.assignmentservice.Model.Todo;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TodoRepository extends MongoRepository<Todo, String> {
}