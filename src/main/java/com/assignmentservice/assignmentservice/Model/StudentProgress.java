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
@Document(collection = "student_progress")
public class StudentProgress {
    @Id
    private String id;

    @NotBlank(message = "Student roll number cannot be empty")
    private String studentRollNumber;

    @NotBlank(message = "Course ID cannot be empty")
    private String courseId;

    @NotNull(message = "Progress percentage cannot be null")
    private Double progressPercentage;

    private Double averageGrade;

    @NotNull(message = "Updated at cannot be null")
    private LocalDateTime updatedAt = LocalDateTime.now();
}