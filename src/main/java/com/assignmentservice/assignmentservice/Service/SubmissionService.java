
package com.assignmentservice.assignmentservice.Service;

import com.assignmentservice.assignmentservice.Model.Submission;
import com.assignmentservice.assignmentservice.Repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SubmissionService {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private FileService fileService;

    public Submission saveSubmission(String userId, String assignmentId, MultipartFile file) throws IOException {

        String submissionId = UUID.randomUUID().toString();

        String fileName = file.getOriginalFilename();
        String fileNo = fileService.uploadSubmissionFile(file, submissionId, assignmentId, fileName);

        Submission submission = new Submission();
        submission.setId(submissionId);
        submission.setUserId(userId);
        submission.setAssignmentId(assignmentId);
        submission.setFileNo(fileNo);
        submission.setSubmittedAt(LocalDateTime.now());

        return submissionRepository.save(submission);
    }

    public void deleteSubmissionByAssignmentIdAndUserId(String assignmentId, String userId) {
        fileService.deleteFileByAssignmentId(assignmentId);
        submissionRepository.deleteByAssignmentIdAndUserId(assignmentId, userId);
    }

    public List<Submission> getAllSubmission() {
        return submissionRepository.findAll();
    }

    public List<Submission> getSubmissionsByAssignmentId(String assignmentId) {
        return submissionRepository.findByAssignmentId(assignmentId);
    }
}
