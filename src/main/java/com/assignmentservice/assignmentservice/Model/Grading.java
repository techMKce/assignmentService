package com.assignmentservice.assignmentservice.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "grading")
public class Grading {
    @Id
    private String id;

    @NotBlank(message = "Assignment ID cannot be empty")
    @Field("assignmentId")
    private String assignmentId;

    @NotBlank(message = "Student name is required")
    @Field("studentName")
    private String studentName;

    @NotBlank(message = "Student department is required")
    @Field("studentDepartment")
    private String studentDepartment;

    @NotBlank(message = "Student roll number is required")
    @Field("studentRollNumber")
    private String studentRollNumber;

    @NotBlank(message = "Grade is required")
    @Field("grade")
    private String grade;

    private String feedback;
    private LocalDateTime gradedAt;

}