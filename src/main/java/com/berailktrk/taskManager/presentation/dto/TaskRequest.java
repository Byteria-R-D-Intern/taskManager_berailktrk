package com.berailktrk.taskManager.presentation.dto;

import java.time.LocalDateTime;

import com.berailktrk.taskManager.domain.model.TaskPriority;
import com.berailktrk.taskManager.domain.model.TaskStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {
    
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private Long assignedToUserId;
    private LocalDateTime dueDate;
    
    // Constructor
    public TaskRequest(String title, String description) {
        this.title = title;
        this.description = description;
    }
} 