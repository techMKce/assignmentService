package com.assignmentservice.assignmentservice.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
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
    
    @NotBlank(message = "Course Cannot be empty")
    private String CourseId;
    
    @NotBlank(message = "Title cannot be empty")
    private String title;

    @NotBlank(message = "Description cannot be empty")
    private String description;

    @NotNull(message = "Due date cannot be null")
    private LocalDateTime dueDate;

    @NotNull(message = "Created at cannot be null")
    private LocalDateTime createdAt = LocalDateTime.now();

    @NotBlank(message = "File number cannot be empty")
    private String fileNo;

    private String resourceLink; // Optional field
}