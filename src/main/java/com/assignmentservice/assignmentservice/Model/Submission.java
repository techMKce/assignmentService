package com.assignmentservice.assignmentservice.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

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

    @NotBlank(message = "Student name is important")
    private String studentName;

    @NotBlank(message = "Student roll number is required")
    private String studentRollNumber;

    private String studentEmail; // Optional

    @NotBlank(message = "Student department is required")
    private String studentDepartment;

    @NotBlank(message = "Student semester is required")
    private String studentSemester;

    @NotNull(message = "Submission time cannot be null")
    private LocalDateTime submittedAt = LocalDateTime.now();

    @NotBlank(message = "File number cannot be empty")
    private String fileNo;

    @NotBlank(message = "File name cannot be empty")
    private String fileName;

    private Long fileSize; // Store in bytes

    @NotBlank(message = "Status cannot be empty")
    private String status = "Accepted";
}