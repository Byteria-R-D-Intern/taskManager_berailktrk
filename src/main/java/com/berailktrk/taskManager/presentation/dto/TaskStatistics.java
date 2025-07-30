package com.berailktrk.taskManager.presentation.dto;

import java.util.Map;

import com.berailktrk.taskManager.domain.model.TaskPriority;
import com.berailktrk.taskManager.domain.model.TaskStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskStatistics {
    private Map<TaskStatus, Long> statusCounts;
    private Map<TaskPriority, Long> priorityCounts;
} 