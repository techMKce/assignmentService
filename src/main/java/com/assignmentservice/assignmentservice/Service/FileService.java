package com.assignmentservice.assignmentservice.Service;

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

        String fileNo = UUID.randomUUID().toString();

        gridFsTemplate.store(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType(),
                new com.mongodb.BasicDBObject("assignmentId", assignmentId)
                        .append("fileNo", fileNo));

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

        String fileNo = UUID.randomUUID().toString();

        gridFsTemplate.store(
                file.getInputStream(),
                fileName,
                file.getContentType(),
                new com.mongodb.BasicDBObject("submissionId", submissionId)
                        .append("assignmentId", assignmentId)
                        .append("fileName", fileName)
                        .append("fileNo", fileNo));

        return fileNo;
    }

    public GridFSFile getFileByAssignmentId(String assignmentId) {
        return gridFsTemplate.findOne(
                new Query(Criteria.where("metadata.assignmentId").is(assignmentId)));
    }

    public GridFSFile getFileBySubmissionId(String submissionId) {
        return gridFsTemplate.findOne(
                new Query(Criteria.where("metadata.submissionId").is(submissionId)));
    }

    public void deleteFileByAssignmentId(String assignmentId) {
        gridFsTemplate.delete(
                new Query(Criteria.where("metadata.assignmentId").is(assignmentId)));
    }

    public void deleteFileBySubmissionId(String submissionId) {
        gridFsTemplate.delete(
                new Query(Criteria.where("metadata.submissionId").is(submissionId)));
    }

    public GridFSFile getFileByFileNo(String fileNo) {
        return gridFsTemplate.findOne(
                new Query(Criteria.where("metadata.fileNo").is(fileNo)));
    }

    public void deleteFileByFileNo(String fileNo) {
        gridFsTemplate.delete(
                new Query(Criteria.where("metadata.fileNo").is(fileNo)));
    }

    public void deleteFileByFileName(String fileName) {
        gridFsTemplate.delete(
                new Query(Criteria.where("filename").is(fileName)));
    }
}