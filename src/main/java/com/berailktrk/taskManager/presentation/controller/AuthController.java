package com.berailktrk.taskManager.presentation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.berailktrk.taskManager.application.usecase.UserService;
import com.berailktrk.taskManager.domain.model.Role;
import com.berailktrk.taskManager.infrastructure.security.JwtProvider;
import com.berailktrk.taskManager.presentation.dto.AuthResponse;
import com.berailktrk.taskManager.presentation.dto.LoginRequest;
import com.berailktrk.taskManager.presentation.dto.RegisterRequest;
import com.berailktrk.taskManager.presentation.dto.ProfileUpdateRequest;
import com.berailktrk.taskManager.presentation.dto.PasswordUpdateRequest;
import com.berailktrk.taskManager.presentation.dto.UsernameUpdateRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;


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

    @Operation(summary = "Kullanıcı kaydı", description = "Yeni bir kullanıcı kaydı oluşturur.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Kayıt başarılı"),
        @ApiResponse(responseCode = "400", description = "Geçersiz istek")
    })
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        userService.register(request.getUsername(), request.getPassword(), Role.ROLE_USER);
        return ResponseEntity.ok("Kayıt başarılı!");
    }

    @Operation(summary = "Kullanıcı girişi", description = "Kullanıcı adı ve şifre ile giriş yapar, JWT token döner.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Giriş başarılı, JWT token döner"),
        @ApiResponse(responseCode = "401", description = "Kullanıcı adı veya şifre hatalı")
    })
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        return userService.authenticate(request.getUsername(), request.getPassword())
            .map(user -> {
                String token = jwtProvider.generateToken(user.getId(), user.getRole().name());
                return ResponseEntity.ok(token);
            })
            .orElseGet(() -> ResponseEntity.status(401).body("Kullanıcı adı veya şifre hatalı!"));
    }

    @Operation(summary = "Profil görüntüleme", description = "JWT token ile giriş yapan kullanıcının profil bilgilerini döner.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profil bilgileri döndü"),
        @ApiResponse(responseCode = "404", description = "Kullanıcı bulunamadı"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim")
    })
    @GetMapping("/profile")
    public ResponseEntity<AuthResponse> getProfile(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtProvider.getUserIdFromToken(token);
        return userService.findById(userId)
            .flatMap(user -> userService.getUserProfile(userId)
                .map(details -> ResponseEntity.ok(new AuthResponse(
                    user.getId(), user.getUsername(), user.getRole().name(),
                    details.getAddress(), details.getPhoneNumber(), details.getBirthDate()
                )))
            ).orElseGet(() -> ResponseEntity.status(404).body(new AuthResponse(null, "Kullanıcı bulunamadı!", null, null, null, null)));
    }

    @Operation(summary = "Profil güncelleme", description = "JWT token ile giriş yapan kullanıcının profil detay bilgilerini günceller.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profil güncellendi"),
        @ApiResponse(responseCode = "404", description = "Kullanıcı bulunamadı veya güncellenemedi"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim")
    })
    @PutMapping("/profile")
    public ResponseEntity<AuthResponse> updateProfile(@RequestHeader("Authorization") String authHeader, @RequestBody ProfileUpdateRequest request) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtProvider.getUserIdFromToken(token);
        return userService.updateUserProfile(userId, request.getAddress(), request.getPhoneNumber(), request.getBirthDate())
            .flatMap(details -> userService.findById(userId)
                .map(user -> ResponseEntity.ok(new AuthResponse(
                    user.getId(), user.getUsername(), user.getRole().name(),
                    details.getAddress(), details.getPhoneNumber(), details.getBirthDate()
                )))
            ).orElseGet(() -> ResponseEntity.status(404).body(new AuthResponse(null, "Kullanıcı bulunamadı veya güncellenemedi!", null, null, null, null)));
    }

    @Operation(summary = "Şifre güncelleme", description = "Kullanıcı mevcut şifresini girerek yeni şifre belirler.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Şifre güncellendi"),
        @ApiResponse(responseCode = "400", description = "Mevcut şifre hatalı veya güncellenemedi"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim")
    })
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestHeader("Authorization") String authHeader, @RequestBody PasswordUpdateRequest request) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtProvider.getUserIdFromToken(token);
        boolean result = userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        if (result) {
            return ResponseEntity.ok("Şifre başarıyla güncellendi.");
        } else {
            return ResponseEntity.badRequest().body("Mevcut şifre hatalı veya güncellenemedi.");
        }
    }

    @Operation(summary = "Kullanıcı adı güncelleme", description = "Kullanıcı yeni bir kullanıcı adı belirler.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Kullanıcı adı güncellendi"),
        @ApiResponse(responseCode = "400", description = "Kullanıcı adı güncellenemedi"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim")
    })
    @PutMapping("/change-username")
    public ResponseEntity<String> changeUsername(@RequestHeader("Authorization") String authHeader, @RequestBody UsernameUpdateRequest request) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtProvider.getUserIdFromToken(token);
        boolean result = userService.changeUsername(userId, request.getNewUsername());
        if (result) {
            return ResponseEntity.ok("Kullanıcı adı başarıyla güncellendi.");
        } else {
            return ResponseEntity.badRequest().body("Kullanıcı adı güncellenemedi.");
        }
    }
}
