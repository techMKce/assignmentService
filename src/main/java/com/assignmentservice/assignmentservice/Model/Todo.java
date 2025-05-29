package com.assignmentservice.assignmentservice.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "todos")
public class Todo {

    @Id
    private String id;

    @NotBlank(message = "Student roll number is required")
    private String studentRollNumber;

    @NotBlank(message = "Assignment ID cannot be empty")
    private String assignmentId;

    @NotBlank(message = "Assignment title cannot be empty")
    private String assignmentTitle;

    @NotBlank(message = "Status cannot be empty")
    private String status; 
}