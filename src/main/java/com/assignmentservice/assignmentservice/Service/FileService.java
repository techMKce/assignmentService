package com.assignmentservice.assignmentservice.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import com.assignmentservice.assignmentservice.Repository.SubmissionRepository;

import java.io.IOException;
import java.util.UUID;

@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    @Autowired
    private GridFsTemplate gridFsTemplate;

    @Autowired
    private SubmissionRepository submissionRepository; // Add this

    public GridFsTemplate getGridFsTemplate() {
        return gridFsTemplate;
    }

    public String uploadFile(MultipartFile file, String assignmentId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }
        if (assignmentId == null || assignmentId.isBlank()) {
            throw new IllegalArgumentException("Assignment ID cannot be null or empty");
        }

        logger.info("Uploading file for assignmentId: {}, originalFilename: {}", assignmentId, file.getOriginalFilename());

        deleteFileByAssignmentId(assignmentId);

        String fileNo = UUID.randomUUID().toString();

        gridFsTemplate.store(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType(),
                new com.mongodb.BasicDBObject("assignmentId", assignmentId)
                        .append("fileNo", fileNo));

        logger.info("File uploaded successfully, fileNo: {}, filename: {}", fileNo, file.getOriginalFilename());
        return fileNo;
    }

    public String uploadSubmissionFile(MultipartFile file, String fileNo, String studentRollNumber, String assignmentId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }
        if (fileNo == null || fileNo.isBlank()) {
            throw new IllegalArgumentException("File number cannot be null or empty");
        }
        if (studentRollNumber == null || studentRollNumber.isBlank()) {
            throw new IllegalArgumentException("Student roll number cannot be null or empty");
        }
        if (assignmentId == null || assignmentId.isBlank()) {
            throw new IllegalArgumentException("Assignment ID cannot be null or empty");
        }

        logger.info("Uploading submission file for studentRollNumber: {}, assignmentId: {}, fileNo: {}", 
                    studentRollNumber, assignmentId, fileNo);

        deleteFileByFileNo(fileNo);

        gridFsTemplate.store(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType(),
                new com.mongodb.BasicDBObject("fileNo", fileNo)
                        .append("studentRollNumber", studentRollNumber)
                        .append("assignmentId", assignmentId));

        logger.info("Submission file uploaded successfully, fileNo: {}", fileNo);
        return fileNo;
    }

    public GridFSFile getFileByAssignmentId(String assignmentId) {
        logger.info("Retrieving file by assignmentId: {}", assignmentId);
        return gridFsTemplate.findOne(
                new Query(Criteria.where("metadata.assignmentId").is(assignmentId)));
    }

    public GridFSFile getFileBySubmissionId(String submissionId) {
        logger.info("Retrieving file by submissionId: {}", submissionId);
        return submissionRepository.findById(submissionId)
                .map(submission -> {
                    String fileNo = submission.getFileNo();
                    logger.info("Found submission, using fileNo: {}", fileNo);
                    GridFSFile file = gridFsTemplate.findOne(
                            new Query(Criteria.where("metadata.fileNo").is(fileNo)));
                    // No file found in GridFS for fileNo: fileNo
                    return file;
                })
                .orElseGet(() -> {
                    logger.error("No submission found for submissionId: {}", submissionId);
                    return null;
                });
    }

    public void deleteFileByAssignmentId(String assignmentId) {
        logger.info("Deleting file for assignmentId: {}", assignmentId);
        gridFsTemplate.delete(
                new Query(Criteria.where("metadata.assignmentId").is(assignmentId)));
        logger.info("Deleted file for assignmentId: {}", assignmentId);
    }

    public void deleteFileBySubmissionId(String submissionId) {
        logger.info("Deleting file for submissionId: {}", submissionId);
        gridFsTemplate.delete(
                new Query(Criteria.where("metadata.submissionId").is(submissionId)));
        logger.info("Deleted file for submissionId: {}", submissionId);
    }

    public GridFSFile getFileByFileNo(String fileNo) {
        logger.info("Retrieving file by fileNo: {}", fileNo);
        return gridFsTemplate.findOne(
                new Query(Criteria.where("metadata.fileNo").is(fileNo)));
    }

    public void deleteFileByFileNo(String fileNo) {
        logger.info("Deleting file for fileNo: {}", fileNo);
        gridFsTemplate.delete(
                new Query(Criteria.where("metadata.fileNo").is(fileNo)));
        logger.info("Deleted file for fileNo: {}", fileNo);
    }
}