package com.berailktrk.taskManager.presentation.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class ProfileUpdateRequest {
    private String address;
    private String phoneNumber;
    private LocalDate birthDate;
} 