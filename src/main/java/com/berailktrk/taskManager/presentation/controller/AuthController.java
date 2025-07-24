package com.berailktrk.taskManager.presentation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.berailktrk.taskManager.application.usecase.UserService;
import com.berailktrk.taskManager.domain.model.Role;
import com.berailktrk.taskManager.infrastructure.security.JwtProvider;
import com.berailktrk.taskManager.presentation.dto.LoginRequest;
import com.berailktrk.taskManager.presentation.dto.RegisterRequest;


@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final JwtProvider jwtProvider;

    @Autowired
    public AuthController(UserService userService, JwtProvider jwtProvider) {
        this.userService = userService;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        userService.register(request.getUsername(), request.getPassword(), Role.USER);
        return ResponseEntity.ok("Kayıt başarılı!");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        return userService.authenticate(request.getUsername(), request.getPassword())
            .map(user -> {
                String token = jwtProvider.generateToken(user.getId(), user.getRole().name());
                return ResponseEntity.ok(token);
            })
            .orElseGet(() -> ResponseEntity.status(401).body("Kullanıcı adı veya şifre hatalı!"));
    }
}
