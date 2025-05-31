package com.assignmentservice.assignmentservice.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "submissions")
public class Submission {

    @Id
    private String id;

    @NotBlank(message = "Assignment ID cannot be empty")
    private String assignmentId;

    @NotBlank(message = "Course ID cannot be empty")
    private String courseId;

    @NotBlank(message = "Course name cannot be empty")
    private String courseName;

    @NotBlank(message = "Course faculty cannot be empty")
    private String courseFaculty;

    @NotBlank(message = "Student name is required")
    private String studentName;

    @NotBlank(message = "Student roll number is required")
    private String studentRollNumber;
    
    @Field("studentEmail")
    private String studentEmail;

    @NotBlank(message = "Student department is required")
    private String studentDepartment;

    @NotBlank(message = "Student semester is required")
    private String studentSemester;

    @NotNull(message = "Submission time cannot be null")
    private LocalDateTime submittedAt = LocalDateTime.now();

    @NotBlank(message = "File number cannot be empty")
    private String fileNo;

    @NotBlank(message = "Status cannot be empty")
    private String status = "Accepted";
}