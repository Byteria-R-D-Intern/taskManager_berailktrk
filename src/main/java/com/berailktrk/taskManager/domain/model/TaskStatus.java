package com.berailktrk.taskManager.domain.model;

public enum TaskStatus {
    PENDING("Bekliyor"),
    IN_PROGRESS("Devam Ediyor"),
    COMPLETED("Tamamlandı"),
    CANCELLED("İptal Edildi");
    
    private final String displayName;
    
    TaskStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
} 