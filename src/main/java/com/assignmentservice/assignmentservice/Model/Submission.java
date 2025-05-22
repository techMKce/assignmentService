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

    @NotBlank(message = "User ID cannot be empty")
    private String userId;

    @NotBlank(message = "Assignment ID cannot be empty")
    private String assignmentId;

    @NotNull(message = "Submission time cannot be null")
    private LocalDateTime submittedAt = LocalDateTime.now();

    @NotBlank(message = "File number cannot be empty")
    private String fileNo;
}