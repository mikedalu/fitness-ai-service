package com.fitness.aiservice.dto;

import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.List;

public class Recommendation {
    private String id;
    private String activityId;
    private String userId;
    private String activityType;
    private String recommendation;
    private List<String> improvements;
    private List<String> suggestions;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
