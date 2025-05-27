package com.assignmentservice.assignmentservice.Repository;

import com.assignmentservice.assignmentservice.Model.StudentProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StudentProgressRepository extends MongoRepository<StudentProgress, String> {
    Optional<StudentProgress> findByStudentRollNumberAndCourseId(String studentRollNumber, String courseId);
}