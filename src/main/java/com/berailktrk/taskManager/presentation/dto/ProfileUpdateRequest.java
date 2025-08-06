package com.berailktrk.taskManager.presentation.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequest {
    private String address;
    private String phoneNumber;
    private LocalDate birthDate;
} 