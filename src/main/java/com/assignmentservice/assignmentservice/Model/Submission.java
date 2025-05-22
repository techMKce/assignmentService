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
@Document(collection = "submission")
public class Submission {
   
    @Id
    private String id;
    
    @NotBlank(message = "User ID cannot be empty")
    private String userId;

    @NotBlank(message = "Assignment ID cannot be empty")
    private String assignmentId;

    @NotNull(message = "Submission time cannot be null")
    private LocalDateTime submittedAt = LocalDateTime.now();

    @NotBlank(message = "File name cannot be empty")
    private String fileName;

    @NotBlank(message = "Roll number cannot be empty")
    private String rollNo;

    @NotBlank(message = "Submission ID cannot be empty")
    private String subId;
}
