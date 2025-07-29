package com.berailktrk.taskManager.presentation.dto;

import java.time.LocalDateTime;

import com.berailktrk.taskManager.domain.model.Task;
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
public class TaskResponse {
    
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private Long assignedToUserId;
    private String assignedToUsername;
    private Long createdByUserId;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime dueDate;
    
    // Constructor
    public TaskResponse(Task task) {
        this.id = task.getId();
        this.title = task.getTitle();
        this.description = task.getDescription();
        this.status = task.getStatus();
        this.priority = task.getPriority();
        this.createdAt = task.getCreatedAt();
        this.updatedAt = task.getUpdatedAt();
        this.dueDate = task.getDueDate();
        
        if (task.getAssignedTo() != null) {
            this.assignedToUserId = task.getAssignedTo().getId();
            this.assignedToUsername = task.getAssignedTo().getUsername();
        }
        
        if (task.getCreatedBy() != null) {
            this.createdByUserId = task.getCreatedBy().getId();
            this.createdByUsername = task.getCreatedBy().getUsername();
        }
    }
} 