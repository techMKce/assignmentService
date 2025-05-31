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

import java.io.IOException;
import java.util.UUID;

@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    @Autowired
    private GridFsTemplate gridFsTemplate;

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

        // Delete any existing file for this assignmentId
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

    public String uploadSubmissionFile(MultipartFile file, String submissionId, String assignmentId, String fileName)
            throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }
        if (submissionId == null || submissionId.isBlank()) {
            throw new IllegalArgumentException("Submission ID cannot be null or empty");
        }
        if (assignmentId == null || assignmentId.isBlank()) {
            throw new IllegalArgumentException("Assignment ID cannot be null or empty");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        logger.info("Uploading submission file for submissionId: {}, assignmentId: {}, fileName: {}", 
                    submissionId, assignmentId, fileName);

        // Delete any existing file for this submissionId
        deleteFileBySubmissionId(submissionId);

        String fileNo = UUID.randomUUID().toString();

        gridFsTemplate.store(
                file.getInputStream(),
                fileName,
                file.getContentType(),
                new com.mongodb.BasicDBObject("submissionId", submissionId)
                        .append("assignmentId", assignmentId)
                        .append("fileName", fileName)
                        .append("fileNo", fileNo));

        logger.info("Submission file uploaded successfully, fileNo: {}", fileNo);
        return fileNo;
    }

    public GridFSFile getFileByAssignmentId(String assignmentId) {
        logger.info("Retrieving file by assignmentId: {}", assignmentId);
        GridFSFile file = gridFsTemplate.findOne(
                new Query(Criteria.where("metadata.assignmentId").is(assignmentId)));
        return file;
    }

    public GridFSFile getFileBySubmissionId(String submissionId) {
        logger.info("Retrieving file by submissionId: {}", submissionId);
        GridFSFile file = gridFsTemplate.findOne(
                new Query(Criteria.where("metadata.submissionId").is(submissionId)));
        
        return file;
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
        GridFSFile file = gridFsTemplate.findOne(
                new Query(Criteria.where("metadata.fileNo").is(fileNo)));
        
        return file;
    }

    public void deleteFileByFileNo(String fileNo) {
        logger.info("Deleting file for fileNo: {}", fileNo);
        gridFsTemplate.delete(
                new Query(Criteria.where("metadata.fileNo").is(fileNo)));
        logger.info("Deleted file for fileNo: {}", fileNo);
    }
}