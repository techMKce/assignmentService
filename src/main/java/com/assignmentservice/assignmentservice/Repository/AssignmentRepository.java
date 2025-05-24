package com.assignmentservice.assignmentservice.Repository;

import com.assignmentservice.assignmentservice.Model.Assignment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssignmentRepository extends MongoRepository<Assignment, String> {

}
