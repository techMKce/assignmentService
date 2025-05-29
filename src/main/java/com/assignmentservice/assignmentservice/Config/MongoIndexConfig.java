package com.assignmentservice.assignmentservice.Config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.CompoundIndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import jakarta.annotation.PostConstruct;
import org.bson.Document;

@Configuration
public class MongoIndexConfig {

    @Autowired
    private MongoTemplate mongoTemplate;

    @PostConstruct
    public void ensureIndexes() {
        // Existing index for grading collection
        IndexOperations gradingIndexOps = mongoTemplate.indexOps("grading");
        CompoundIndexDefinition gradingIndex = new CompoundIndexDefinition(
                new Document("studentRollNumber", 1).append("assignmentId", 1)
        );
        gradingIndex.unique();
        gradingIndexOps.ensureIndex(gradingIndex);

    }
}