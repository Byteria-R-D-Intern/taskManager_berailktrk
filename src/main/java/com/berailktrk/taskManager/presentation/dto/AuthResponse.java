package com.berailktrk.taskManager.presentation.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AuthResponse {
    private String message;
    private String token;
    private Long id;
    private String username;
    private String role;
    private String address;
    private String phoneNumber;
    private LocalDate birthDate;

    public AuthResponse(Long id, String username, String role, String address, String phoneNumber, LocalDate birthDate) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.birthDate = birthDate;
    }
}
