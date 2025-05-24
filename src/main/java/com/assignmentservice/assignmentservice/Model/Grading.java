package com.assignmentservice.assignmentservice.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "grading")
public class Grading {
    @Id
    private String id;
    private String userId;
    private String assignmentId;
    private String studentName;
    private String studentRollNumber;
    private String grade;
    private String feedback;
    private LocalDateTime gradedAt;
}
