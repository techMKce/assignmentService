package com.assignmentservice.assignmentservice.Service;

import com.assignmentservice.assignmentservice.Model.Assignment;
import com.assignmentservice.assignmentservice.Model.Submission;
import com.assignmentservice.assignmentservice.Model.Todo;
import com.assignmentservice.assignmentservice.Repository.GradingRepository;
import com.assignmentservice.assignmentservice.Repository.SubmissionRepository;
import com.assignmentservice.assignmentservice.Repository.TodoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

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
            "C", 40.0
    );

    public Submission saveSubmission(String assignmentId, String studentName, String studentRollNumber, String studentDepartment, String studentSemester, MultipartFile file) throws IOException {
        log.info("Attempting to save submission for studentRollNumber: {}, assignmentId: {}", studentRollNumber, assignmentId);

        assignmentService.getAssignmentById(
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
        submission.setStudentName(studentName);
        submission.setStudentRollNumber(studentRollNumber);
        submission.setStudentDepartment(studentDepartment);
        submission.setStudentSemester(studentSemester);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setFileNo(fileNo);
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
        log.info("Successfully updated submission status to {} for submissionId: {}", status, submissionId);

        todoRepository.deleteByStudentRollNumberAndAssignmentId(submission.getStudentRollNumber(), submission.getAssignmentId());

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

    public List<Submission> getSubmissionsByAssignmentId(String assignmentId) {
        return submissionRepository.findByAssignmentId(
                Optional.ofNullable(assignmentId)
                        .filter(id -> !id.isBlank())
                        .orElseThrow(() -> new IllegalArgumentException("Assignment ID cannot be null or blank"))
        );
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
                    submissionRepository.deleteByAssignmentIdAndStudentRollNumber(validAssignmentId, validStudentRollNumber);
                    todoRepository.deleteByStudentRollNumberAndAssignmentId(validStudentRollNumber, validAssignmentId);
                    fileService.deleteFileByFileNo(submission.getFileNo());
                    log.info("Deleted submission for studentRollNumber: {}, assignmentId: {}", validStudentRollNumber, validAssignmentId);
                });
    }

    public long countByStudentRollNumberAndAssignmentIds(String studentRollNumber, List<String> assignmentIds) {
        return submissionRepository.countByStudentRollNumberAndAssignmentIdInAndStatusAccepted(
                Optional.ofNullable(studentRollNumber)
                        .filter(id -> !id.isBlank())
                        .orElseThrow(() -> new IllegalArgumentException("Student Roll Number cannot be null or blank")),
                Optional.ofNullable(assignmentIds)
                        .filter(ids -> !ids.isEmpty())
                        .orElseThrow(() -> new IllegalArgumentException("Assignment IDs cannot be null or empty"))
        );
    }

    public List<StudentProgress> getStudentProgressForCourse(String courseId) {
        List<Assignment> assignments = assignmentService.getAssignmentsByCourseId(
                Optional.ofNullable(courseId)
                        .filter(id -> !id.isBlank())
                        .orElseThrow(() -> new IllegalArgumentException("Course ID cannot be null or blank"))
        );

        List<String> assignmentIds = assignments.stream()
                .map(Assignment::getAssignmentId)
                .toList();

        long totalAssignments = assignments.size();

        List<String> studentRollNumbers = submissionRepository.findDistinctStudentRollNumbersByAssignmentIds(assignmentIds)
                .stream()
                .map(Submission::getStudentRollNumber)
                .distinct()
                .toList();

        return studentRollNumbers.stream()
                .map(studentRollNumber -> {
                    double progressPercentage = Optional.of(totalAssignments)
                            .filter(total -> total > 0)
                            .map(total -> (double) submissionRepository.countByStudentRollNumberAndAssignmentIdInAndStatusAccepted(studentRollNumber, assignmentIds) / total * 100)
                            .map(percentage -> Math.round(percentage * 100.0) / 100.0)
                            .orElse(0.0);

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

                    Double averageGrade = Optional.of(gradedAssignments)
                            .filter(count -> count > 0)
                            .map(count -> Math.round((totalGrade / count) * 100.0) / 100.0)
                            .orElse(null);

                    StudentProgress progress = new StudentProgress();
                    progress.setStudentRollNumber(studentRollNumber);
                    progress.setProgressPercentage(progressPercentage);
                    progress.setAverageGrade(averageGrade);

                    Optional<Submission> firstSubmission = submissionRepository.findByStudentRollNumberAndAssignmentIdIn(studentRollNumber, assignmentIds)
                            .stream()
                            .findFirst();

                    progress.setStudentName(firstSubmission.map(Submission::getStudentName).orElse("Unknown"));
                    progress.setStudentDepartment(firstSubmission.map(Submission::getStudentDepartment).orElse("Unknown"));
                    progress.setStudentSemester(firstSubmission.map(Submission::getStudentSemester).orElse("Unknown"));

                    Optional.of(firstSubmission)
                            .filter(Optional::isEmpty)
                            .ifPresent(sub -> log.warn("No submission found for studentRollNumber: {} in courseId: {}", studentRollNumber, courseId));

                    log.info("Calculated progress {}% and average grade {} for studentRollNumber: {}, courseId: {}",
                            progressPercentage, Optional.ofNullable(averageGrade).map(Object::toString).orElse("Not graded"), studentRollNumber, courseId);
                    return progress;
                })
                .toList();
    }

    private double convertLetterGradeToNumber(String grade) {
        return Optional.ofNullable(grade)
                .filter(g -> !g.isBlank())
                .map(String::toUpperCase)
                .map(GRADE_MAP::get)
                .orElseThrow(() -> new IllegalArgumentException("Invalid grade: " + grade));
    }

    public static class StudentProgress {
        private String studentRollNumber;
        private double progressPercentage;
        private Double averageGrade;
        private String studentName;
        private String studentDepartment;
        private String studentSemester;

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
    }
}