package com.assignmentservice.assignmentservice.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "assignments")
public class Assignment {
    @Id
    private String assignmentId;

    @NotBlank(message = "Course ID cannot be empty")
    @Field("courseId")
    private String courseId;

    @NotBlank(message = "Course name cannot be empty")
    private String courseName;

    @NotBlank(message = "Course faculty cannot be empty")
    private String courseFaculty;

    @NotBlank(message = "Title cannot be empty")
    private String title;

    @NotBlank(message = "Description cannot be empty")
    private String description;

    @NotNull(message = "Due date cannot be null")
    private LocalDateTime dueDate;

    @NotNull(message = "Created at cannot be null")
    private LocalDateTime createdAt = LocalDateTime.now();

    @NotBlank(message = "FileNo cannot be empty")
    private String fileNo;

    @NotBlank(message = "FileName cannot be empty")
    private String fileName;

    private String resourceLink;
}