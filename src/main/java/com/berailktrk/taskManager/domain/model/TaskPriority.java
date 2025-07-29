package com.berailktrk.taskManager.domain.model;

public enum TaskPriority {
    LOW("Düşük"),
    MEDIUM("Orta"),
    HIGH("Yüksek"),
    URGENT("Acil");
    
    private final String displayName;
    
    TaskPriority(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
} 