package com.assignmentservice.assignmentservice.Service;

import com.assignmentservice.assignmentservice.Model.Assignment;
import com.assignmentservice.assignmentservice.Model.Submission;
import com.assignmentservice.assignmentservice.Model.Todo;
import com.assignmentservice.assignmentservice.Repository.GradingRepository;
import com.assignmentservice.assignmentservice.Repository.SubmissionRepository;
import com.assignmentservice.assignmentservice.Repository.TodoRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;

@Service
@Slf4j
public class SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private FileService fileService;

    @Autowired
    private GradingRepository gradingRepository;

    private static final Map<String, Double> GRADE_MAP = Map.of(
            "O", 100.0,
            "A+", 90.0,
            "A", 80.0,
            "B+", 70.0,
            "B", 60.0,
            "C+", 50.0,
            "C", 40.0);

   public Submission saveSubmission(String assignmentId, String studentName, String studentRollNumber, String studentEmail, String studentDepartment, String studentSemester, MultipartFile file) throws IOException {
    log.info("Attempting to save submission for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId);

    Assignment assignment = assignmentService.getAssignmentById(
            Optional.ofNullable(assignmentId)
                    .filter(id -> !id.isBlank())
                    .orElseThrow(() -> new IllegalArgumentException("Assignment ID cannot be null or blank"))
    ).orElseThrow(() -> new IllegalArgumentException("Assignment not found for ID: " + assignmentId));

    submissionRepository.findByAssignmentIdAndStudentRollNumber(assignmentId, studentRollNumber)
            .ifPresent(submission -> {
                submissionRepository.delete(submission);
                todoRepository.deleteByStudentRollNumberAndAssignmentId(studentRollNumber, assignmentId);
                fileService.deleteFileByFileNo(submission.getFileNo());
                log.info("Deleted existing submission for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId);
            });

    String fileNo = fileService.uploadSubmissionFile(file, UUID.randomUUID().toString(), studentRollNumber, assignmentId);
    Submission submission = new Submission();
    submission.setId(UUID.randomUUID().toString());
    submission.setAssignmentId(assignmentId);
    submission.setCourseId(assignment.getCourseId());
    submission.setCourseName(assignment.getCourseName());
    submission.setCourseFaculty(assignment.getCourseFaculty());
    submission.setStudentName(studentName);
    submission.setStudentRollNumber(studentRollNumber);
    submission.setStudentEmail(studentEmail);
    submission.setStudentDepartment(studentDepartment);
    submission.setStudentSemester(studentSemester);
    submission.setSubmittedAt(LocalDateTime.now());
    submission.setFileNo(fileNo);
    submission.setFileName(file.getOriginalFilename());
    submission.setFileSize(file.getSize()); 
    submission.setStatus("Accepted");

    Submission savedSubmission = submissionRepository.save(submission);
    log.info("Successfully saved submission for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId);
    return savedSubmission;
}

    public Submission updateSubmissionStatus(String submissionId, String status, String assignmentTitle) {
        Submission submission = Optional.ofNullable(submissionId)
                .filter(id -> !id.isBlank())
                .map(submissionRepository::findById)
                .map(Optional::get)
                .orElseThrow(() -> new IllegalArgumentException("Submission ID cannot be null or blank"));

        Optional.ofNullable(status)
                .filter(s -> List.of("Accepted", "Rejected").contains(s))
                .orElseThrow(() -> new IllegalArgumentException("Status must be either 'Accepted' or 'Rejected'"));

        submission.setStatus(status);
        Submission updatedSubmission = submissionRepository.save(submission);

        if (status.equals("Rejected")) {
            boolean mailStatus = sendMail(assignmentTitle, updatedSubmission);
            log.info("Mail sent status: {}", mailStatus);
        }
        log.info("Successfully updated submission status to {} for submissionId: {}", status, submissionId);

        todoRepository.deleteByStudentRollNumberAndAssignmentId(submission.getStudentRollNumber(),
                submission.getAssignmentId());

        Optional.of(status)
                .filter(s -> s.equals("Rejected"))
                .ifPresent(s -> {
                    Todo todo = new Todo();
                    todo.setId(UUID.randomUUID().toString());
                    todo.setStudentRollNumber(submission.getStudentRollNumber());
                    todo.setAssignmentId(submission.getAssignmentId());
                    todo.setAssignmentTitle(assignmentTitle);
                    todo.setStatus("Pending");
                    todoRepository.save(todo);
                    log.info("Created todo for rejected submission: studentRollNumber={}, assignmentId={}",
                            submission.getStudentRollNumber(), submission.getAssignmentId());
                });

        return updatedSubmission;
    }

    private boolean sendMail(String assignmentTitle, Submission submission) {
        RestTemplate restTemplate = new RestTemplate();
        String emailServiceUrl = "http://localhost:8080/api/v1/email/sendRejectEmail";

        URI uri = UriComponentsBuilder.fromUriString(emailServiceUrl)
                .queryParam("id", submission.getStudentRollNumber())
                .build()
                .toUri();

        Map<String, String> requestBody = getStringStringMap(assignmentTitle, submission);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Boolean> response = restTemplate.postForEntity(
                uri,
                requestEntity,
                Boolean.class);
        return Boolean.TRUE.equals(response.getBody());
    }

    private static Map<String, String> getStringStringMap(String assignmentTitle, Submission submission) {
        String emailBody = "Your submission for the assignment '" + assignmentTitle + "' (Assignment ID: "
                + submission.getAssignmentId() +
                ") has been rejected.\n\nAssignment Details:\n" +
                "Assignment ID: " + submission.getAssignmentId() + "\n" +
                "Course: " + submission.getCourseName() + "\n" +
                "Student Name: " + submission.getStudentName() + "\n" +
                "Student Roll Number: " + submission.getStudentRollNumber() + "\n" +
                "Student Email: " + Optional.ofNullable(submission.getStudentEmail()).orElse("Not provided") + "\n" +
                "File Number: " + submission.getFileNo() + "\n" +
                "Status: " + submission.getStatus() + "\n\n" +
                "Please review the feedback and resubmit if necessary.";
        String emailSubject = "Submission Rejected: " + assignmentTitle;
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("subject", emailSubject);
        requestBody.put("body", emailBody);
        return requestBody;
    }

    public List<Submission> getSubmissionsByAssignmentId(String assignmentId) {
        log.info("Fetching submissions for assignmentId: {}", assignmentId);
        String validAssignmentId = Optional.ofNullable(assignmentId)
                .filter(assgnId -> !assgnId.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Assignment ID cannot be null or blank"));

        Assignment assignment = assignmentService.getAssignmentById(validAssignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found for ID: " + validAssignmentId));
        log.info("Assignment found: title={}", assignment.getTitle());

        List<Submission> submissions = submissionRepository.findByAssignmentId(validAssignmentId);
        log.info("Found {} submissions for assignmentId: {}", submissions.size(), validAssignmentId);
        return submissions;
    }

    public Submission getSubmissionById(String submissionId) {
        return Optional.ofNullable(submissionId)
                .filter(id -> !id.isBlank())
                .map(submissionRepository::findById)
                .map(Optional::get)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found for ID: " + submissionId));
    }

    public void deleteSubmissionByAssignmentIdAndStudentRollNumber(String assignmentId, String studentRollNumber) {
        String validAssignmentId = Optional.ofNullable(assignmentId)
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Assignment ID cannot be null or blank"));
        String validStudentRollNumber = Optional.ofNullable(studentRollNumber)
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Student Roll Number cannot be null or blank"));

        submissionRepository.findByAssignmentIdAndStudentRollNumber(validAssignmentId, validStudentRollNumber)
                .ifPresent(submission -> {
                    submissionRepository.deleteByAssignmentIdAndStudentRollNumber(validAssignmentId,
                            validStudentRollNumber);
                    todoRepository.deleteByStudentRollNumberAndAssignmentId(validStudentRollNumber, validAssignmentId);
                    fileService.deleteFileByFileNo(submission.getFileNo());
                    log.info("Deleted submission for studentRollNumber: {}, assignmentId: {}", validStudentRollNumber,
                            validAssignmentId);
                });
    }

    public long countByStudentRollNumberAndAssignmentIds(String studentRollNumber, List<String> assignmentIds) {
        return submissionRepository.countByStudentRollNumberAndAssignmentIdInAndStatusAccepted(
                Optional.ofNullable(studentRollNumber)
                        .filter(id -> !id.isBlank())
                        .orElseThrow(() -> new IllegalArgumentException("Student Roll Number cannot be null or blank")),
                Optional.ofNullable(assignmentIds)
                        .filter(ids -> !ids.isEmpty())
                        .orElseThrow(() -> new IllegalArgumentException("Assignment IDs cannot be null or empty")));
    }

    public List<StudentProgress> getStudentProgressForCourse(String courseId) {
        List<Assignment> assignments = assignmentService.getAssignmentsByCourseId(
                Optional.ofNullable(courseId)
                        .filter(id -> !id.isBlank())
                        .orElseThrow(() -> new IllegalArgumentException("Course ID cannot be null or blank")));

        List<String> assignmentIds = assignments.stream()
                .map(Assignment::getAssignmentId)
                .toList();

        long totalAssignments = assignments.size();

        List<String> studentRollNumbers = submissionRepository
                .findDistinctStudentRollNumbersByAssignmentIds(assignmentIds)
                .stream()
                .map(Submission::getStudentRollNumber)
                .distinct()
                .toList();

        return studentRollNumbers.stream()
                .map(studentRollNumber -> {
                    double progressPercentage = totalAssignments > 0 ?
                            ((double) submissionRepository
                                    .countByStudentRollNumberAndAssignmentIdInAndStatusAccepted(studentRollNumber, assignmentIds)
                                    / totalAssignments) * 100 :
                            0.0;
                    progressPercentage = Math.round(progressPercentage * 100.0) / 100.0;

                    double totalGrade = assignmentIds.stream()
                            .map(id -> gradingRepository.findByStudentRollNumberAndAssignmentId(studentRollNumber, id))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .map(grading -> grading.getGrade())
                            .filter(grade -> grade != null && !grade.isBlank())
                            .mapToDouble(this::convertLetterGradeToNumber)
                            .sum();

                    long gradedAssignments = assignmentIds.stream()
                            .map(id -> gradingRepository.findByStudentRollNumberAndAssignmentId(studentRollNumber, id))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .map(grading -> grading.getGrade())
                            .filter(grade -> grade != null && !grade.isBlank())
                            .count();

                    Double averageGrade = gradedAssignments > 0 ?
                            Math.round((totalGrade / gradedAssignments) * 100.0) / 100.0 :
                            null;

                    StudentProgress progress = new StudentProgress();
                    progress.setStudentRollNumber(studentRollNumber);
                    progress.setProgressPercentage(progressPercentage);
                    progress.setAverageGrade(averageGrade);

                    Optional<Submission> firstSubmission = submissionRepository
                            .findByStudentRollNumberAndAssignmentIdIn(studentRollNumber, assignmentIds)
                            .stream()
                            .findFirst();

                    progress.setStudentName(firstSubmission.map(Submission::getStudentName).orElse("Unknown"));
                    progress.setStudentEmail(firstSubmission.map(Submission::getStudentEmail).orElse("Unknown"));
                    progress.setStudentDepartment(firstSubmission.map(Submission::getStudentDepartment).orElse("Unknown"));
                    progress.setStudentSemester(firstSubmission.map(Submission::getStudentSemester).orElse("Unknown"));

                    Optional.of(firstSubmission)
                            .filter(Optional::isEmpty)
                            .ifPresent(sub -> log.warn("No submission found for studentRollNumber: {} in courseId: {}",
                                    studentRollNumber, courseId));

                    // Populate assignmentGrades
                     List<StudentProgress.AssignmentGrade> assignmentGrades = assignments.stream()
                            .map(assignment -> {
                                StudentProgress.AssignmentGrade assignmentGrade = new StudentProgress.AssignmentGrade();
                                assignmentGrade.setAssignmentTitle(assignment.getTitle());
                                double grade = gradingRepository.findByStudentRollNumberAndAssignmentId(
                                        studentRollNumber, assignment.getAssignmentId())
                                        .map(g -> Optional.ofNullable(g.getGrade())
                                                .filter(gr -> !gr.isBlank())
                                                .map(this::convertLetterGradeToNumber)
                                                .orElse(0.0))
                                        .orElse(0.0);
                                assignmentGrade.setGrade(grade);
                                return assignmentGrade;
                            })
                            .toList();
                    progress.setAssignmentGrades(assignmentGrades);

                    log.info("Calculated progress {}% and average grade {} for studentRollNumber: {}, courseId: {}",
                            progressPercentage,
                            Optional.ofNullable(averageGrade).map(Object::toString).orElse("Not graded"),
                            studentRollNumber, courseId);
                    return progress;
                })
                .toList();
    }

    public String generateStudentProgressCsvForCourse(String courseId) throws IOException {
        String validCourseId = Optional.ofNullable(courseId)
                .filter(id -> !id.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("Course ID cannot be null or empty"));

        List<StudentProgress> progressList = getStudentProgressForCourse(validCourseId);
        if (progressList.isEmpty()) {
            throw new IllegalArgumentException("No student progress data found for course ID: " + validCourseId);
        }

        List<Assignment> assignments = assignmentService.getAssignmentsByCourseId(validCourseId);
        String courseName = assignments.isEmpty() ? "Unknown" : assignments.get(0).getCourseName();
        String courseFaculty = assignments.isEmpty() ? "Unknown" : assignments.get(0).getCourseFaculty();

        StringWriter writer = new StringWriter();
        StringBuilder header = new StringBuilder(
                "S.No,Student Name,Student Roll Number,Student Email,Student Department,Student Semester,Course Name,Course Faculty,Progress Percentage,Average Grade");
        for (Assignment assignment : assignments) {
            header.append(",").append(escapeCsv(assignment.getTitle()));
        }
        header.append("\n");
        writer.write(header.toString());

        IntStream.range(0, progressList.size()).forEach(i -> {
            StudentProgress progress = progressList.get(i);
            StringBuilder row = new StringBuilder(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%.2f,%s",
                    i + 1,
                    escapeCsv(Optional.ofNullable(progress.getStudentName()).orElse("Unknown")),
                    escapeCsv(Optional.ofNullable(progress.getStudentRollNumber()).orElse("Unknown")),
                    escapeCsv(Optional.ofNullable(progress.getStudentEmail()).orElse("Not provided")),
                    escapeCsv(Optional.ofNullable(progress.getStudentDepartment()).orElse("Unknown")),
                    escapeCsv(Optional.ofNullable(progress.getStudentSemester()).orElse("Unknown")),
                    escapeCsv(courseName),
                    escapeCsv(courseFaculty),
                    progress.getProgressPercentage(),
                    escapeCsv(Optional.ofNullable(progress.getAverageGrade())
                            .map(Object::toString)
                            .orElse("Not Graded"))));

            for (Assignment assignment : assignments) {
                String assignmentId = assignment.getAssignmentId();
                Optional<com.assignmentservice.assignmentservice.Model.Grading> grading = 
                    gradingRepository.findByStudentRollNumberAndAssignmentId(
                        progress.getStudentRollNumber(), assignmentId);
                double gradeValue = grading
                        .map(g -> Optional.ofNullable(g.getGrade())
                                .filter(grade -> !grade.isBlank())
                                .map(this::convertLetterGradeToNumber)
                                .orElse(0.0))
                        .orElse(0.0);
                row.append(",").append(String.format("%.2f", gradeValue));
            }
            row.append("\n");
            writer.write(row.toString());
        });

        log.info("Generated CSV for courseId: {} with {} students and {} assignments", 
                validCourseId, progressList.size(), assignments.size());
        return writer.toString();
    }

    private double convertLetterGradeToNumber(String grade) {
        return Optional.ofNullable(grade)
                .filter(g -> !g.isBlank())
                .map(String::toUpperCase)
                .map(GRADE_MAP::get)
                .orElseThrow(() -> new IllegalArgumentException("Invalid grade: " + grade));
    }

    private String escapeCsv(String value) {
        return Optional.ofNullable(value)
                .map(v -> v.contains(",") || v.contains("\"") || v.contains("\n")
                        ? "\"" + v.replace("\"", "\"\"") + "\""
                        : v)
                .orElse("");
    }

    public static class StudentProgress {
        private String studentRollNumber;
        private double progressPercentage;
        private Double averageGrade;
        private String studentName;
        private String studentEmail;
        private String studentDepartment;
        private String studentSemester;
        private List<AssignmentGrade> assignmentGrades;

        public String getStudentRollNumber() {
            return studentRollNumber;
        }

        public void setStudentRollNumber(String studentRollNumber) {
            this.studentRollNumber = studentRollNumber;
        }

        public double getProgressPercentage() {
            return progressPercentage;
        }

        public void setProgressPercentage(double progressPercentage) {
            this.progressPercentage = progressPercentage;
        }

        public Double getAverageGrade() {
            return averageGrade;
        }

        public void setAverageGrade(Double averageGrade) {
            this.averageGrade = averageGrade;
        }

        public String getStudentName() {
            return studentName;
        }

        public void setStudentName(String studentName) {
            this.studentName = studentName;
        }

        public String getStudentEmail() {
            return studentEmail;
        }

        public void setStudentEmail(String studentEmail) {
            this.studentEmail = studentEmail;
        }

        public String getStudentDepartment() {
            return studentDepartment;
        }

        public void setStudentDepartment(String studentDepartment) {
            this.studentDepartment = studentDepartment;
        }

        public String getStudentSemester() {
            return studentSemester;
        }

        public void setStudentSemester(String studentSemester) {
            this.studentSemester = studentSemester;
        }

        public List<AssignmentGrade> getAssignmentGrades() {
            return assignmentGrades;
        }

        public void setAssignmentGrades(List<AssignmentGrade> assignmentGrades) {
            this.assignmentGrades = assignmentGrades;
        }

        public static class AssignmentGrade {
            private String assignmentTitle;
            private double grade;

            public String getAssignmentTitle() {
                return assignmentTitle;
            }

            public void setAssignmentTitle(String assignmentTitle) {
                this.assignmentTitle = assignmentTitle;
            }

            public double getGrade() {
                return grade;
            }

            public void setGrade(double grade) {
                this.grade = grade;
            }
        }
    }
}