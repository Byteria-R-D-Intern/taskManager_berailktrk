package com.berailktrk.taskManager.presentation.dto;

import com.berailktrk.taskManager.domain.model.TaskPriority;
import com.berailktrk.taskManager.domain.model.TaskStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskSearchRequest {
    private String title;
    private TaskStatus status;
    private TaskPriority priority;
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "createdAt";
    private String sortDirection = "desc";
} 