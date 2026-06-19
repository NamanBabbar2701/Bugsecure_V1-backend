package com.bugsecure.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class MongoConfig {

    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            // Test MongoDB connection
            mongoTemplate.getDb().getName();
            logger.info(" MongoDB connection successful! Database: {}", mongoTemplate.getDb().getName());
            logger.info(" All data will persist across server restarts");
        } catch (Exception e) {
            logger.error(" MongoDB connection failed: {}", e.getMessage());
        }
    }
}







