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
        // Create a unique compound index on userId and assignmentId for the Grading collection
        IndexOperations indexOps = mongoTemplate.indexOps("Grading");
        CompoundIndexDefinition indexDefinition = new CompoundIndexDefinition(
            new Document("userId", 1).append("assignmentId", 1)
        );
        indexDefinition.unique();
        indexOps.ensureIndex(indexDefinition);
    }
}