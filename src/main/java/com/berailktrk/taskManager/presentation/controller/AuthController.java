package com.berailktrk.taskManager.presentation.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.berailktrk.taskManager.application.usecase.UserService;
import com.berailktrk.taskManager.domain.model.User;
import com.berailktrk.taskManager.infrastructure.security.JwtProvider;
import com.berailktrk.taskManager.presentation.dto.AuthResponse;
import com.berailktrk.taskManager.presentation.dto.LoginRequest;
import com.berailktrk.taskManager.presentation.dto.PasswordUpdateRequest;
import com.berailktrk.taskManager.presentation.dto.ProfileUpdateRequest;
import com.berailktrk.taskManager.presentation.dto.RegisterRequest;
import com.berailktrk.taskManager.presentation.dto.UsernameUpdateRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

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
        try {
            User user = userService.register(request);
            return ResponseEntity.ok("Kayıt başarılı! Kullanıcı ID: " + user.getId());
        } catch (RuntimeException e) {
            String message = e.getMessage();
            
            // Validation hataları
            if (message.contains("boş olamaz") || message.contains("uzun olamaz")) {
                return ResponseEntity.badRequest().body("Validation Error: " + message);
            }
            
            // Genel hata
            return ResponseEntity.badRequest().body("Error: " + message);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server Error: " + e.getMessage());
        }
    }

    @Operation(summary = "Kullanıcı girişi", description = "Kullanıcı adı ve şifre ile giriş yapar, JWT token döner.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Giriş başarılı, JWT token döner"),
        @ApiResponse(responseCode = "401", description = "Kullanıcı adı veya şifre hatalı")
    })
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        try {
            return userService.authenticate(request.getUsername(), request.getPassword())
                .map(user -> {
                    String token = jwtProvider.generateToken(user.getId(), user.getRole().name());
                    return ResponseEntity.ok(token);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Kullanıcı adı veya şifre hatalı!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server Error: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Profil görüntüleme", 
        description = "JWT token ile giriş yapan kullanıcının profil bilgilerini döner. Admin ve Manager'lar başka kullanıcıların profilini de görebilir.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profil bilgileri döndü"),
        @ApiResponse(responseCode = "404", description = "Kullanıcı bulunamadı"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim"),
        @ApiResponse(responseCode = "403", description = "Bu kullanıcının profilini görme yetkiniz yok")
    })
    @GetMapping("/profile")
    public ResponseEntity<AuthResponse> getProfile(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(required = false) Long userId
    ) {
        try {
            // Token kontrolü
            if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(null, "Authorization header is missing", null, null, null, null));
            }
            
            // Token'ı al (Bearer prefix'i varsa kaldır, yoksa direkt kullan)
            String token;
            if (authorizationHeader.startsWith("Bearer ")) {
                token = authorizationHeader.substring(7);
            } else {
                token = authorizationHeader.trim();
            }
            
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(null, "Token is invalid or expired", null, null, null, null));
            }
            
            // Token'dan kullanıcı bilgilerini al
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            String currentUserRole = jwtProvider.getUserRoleFromToken(token);
            
            // Eğer userId parametresi verilmemişse, kullanıcı kendi profilini görüyor
            Long targetUserId = (userId != null) ? userId : currentUserId;
            
            // Yetki kontrolü
            if (userId != null && !currentUserRole.equals("ROLE_ADMIN") && !currentUserRole.equals("ROLE_MANAGER")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AuthResponse("Bu kullanıcının profilini görme yetkiniz yok!"));
            }
            
            return userService.findById(targetUserId)
                .flatMap(user -> userService.getUserProfile(targetUserId)
                    .map(details -> ResponseEntity.ok(new AuthResponse(
                        user.getId(), user.getUsername(), user.getRole().name(),
                        details.getAddress(), details.getPhoneNumber(), details.getBirthDate()
                    )))
                ).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(new AuthResponse("Kullanıcı bulunamadı!")));
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponse(null, "Bir hata oluştu: " + e.getMessage(), null, null, null, null));
        }
    }

    @Operation(
        summary = "Tüm kullanıcıları listele", 
        description = "Admin ve Manager'lar tüm kullanıcıları listeleyebilir.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Kullanıcı listesi döndü"),
        @ApiResponse(responseCode = "403", description = "Bu işlemi yapma yetkiniz yok")
    })
    @GetMapping("/users")
    public ResponseEntity<String> getAllUsers(@RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        try {
            // Token kontrolü
            if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header is missing");
            }
            
            // Token'ı al (Bearer prefix'i varsa kaldır, yoksa direkt kullan)
            String token;
            if (authorizationHeader.startsWith("Bearer ")) {
                token = authorizationHeader.substring(7);
            } else {
                token = authorizationHeader.trim();
            }
            
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is invalid or expired");
            }
            
            String currentUserRole = jwtProvider.getUserRoleFromToken(token);
            
            if (!currentUserRole.equals("ROLE_ADMIN") && !currentUserRole.equals("ROLE_MANAGER")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bu işlemi yapma yetkiniz yok!");
            }
            
            // Tüm kullanıcıları getir
            var users = userService.getAllUsers();
            StringBuilder response = new StringBuilder();
            response.append("Kullanıcı Listesi:\n");
            
            for (var user : users) {
                response.append("ID: ").append(user.getId())
                       .append(", Kullanıcı Adı: ").append(user.getUsername())
                       .append(", Rol: ").append(user.getRole().name())
                       .append("\n");
            }
            
            return ResponseEntity.ok(response.toString());
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Bir hata oluştu: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Profil güncelleme", 
        description = "JWT token ile giriş yapan kullanıcının profil detay bilgilerini günceller.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profil güncellendi"),
        @ApiResponse(responseCode = "404", description = "Kullanıcı bulunamadı veya güncellenemedi"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim")
    })
    @PutMapping("/profile")
    public ResponseEntity<AuthResponse> updateProfile(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody ProfileUpdateRequest request,
        @RequestParam(required = false) Long userId
    ) {
        try {
            // Token kontrolü
            if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(null, "Authorization header is missing", null, null, null, null));
            }
            // Token'ı al (Bearer prefix'i varsa kaldır, yoksa direkt kullan)
            String token;
            if (authorizationHeader.startsWith("Bearer ")) {
                token = authorizationHeader.substring(7);
            } else {
                token = authorizationHeader.trim();
            }
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(null, "Token is invalid or expired", null, null, null, null));
            }
            
            Long userId = jwtProvider.getUserIdFromToken(token);
            
            return userService.updateUserProfile(userId, request.getAddress(), request.getPhoneNumber(), request.getBirthDate())
                .flatMap(details -> userService.findById(userId)
                    .map(user -> ResponseEntity.ok(new AuthResponse(
                        user.getId(), user.getUsername(), user.getRole().name(),
                        details.getAddress(), details.getPhoneNumber(), details.getBirthDate()
                    )))
                ).orElseGet(() -> ResponseEntity.status(404).body(new AuthResponse(null, "Kullanıcı bulunamadı veya güncellenemedi!", null, null, null, null)));
                
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthResponse(null, "Bir hata oluştu: " + e.getMessage(), null, null, null, null));
        }
    }

    @Operation(
        summary = "Şifre güncelleme", 
        description = "Kullanıcı mevcut şifresini girerek yeni şifre belirler.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Şifre güncellendi"),
        @ApiResponse(responseCode = "400", description = "Mevcut şifre hatalı veya güncellenemedi"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim")
    })
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody PasswordUpdateRequest request
    ) {
        try {
            // Token kontrolü
            if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header is missing");
            }
            
            // Token'ı al (Bearer prefix'i varsa kaldır, yoksa direkt kullan)
            String token;
            if (authorizationHeader.startsWith("Bearer ")) {
                token = authorizationHeader.substring(7);
            } else {
                token = authorizationHeader.trim();
            }
            
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is invalid or expired");
            }
            
            Long userId = jwtProvider.getUserIdFromToken(token);
            boolean result = userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
            
            if (result) {
                return ResponseEntity.ok("Şifre başarıyla güncellendi.");
            } else {
                return ResponseEntity.badRequest().body("Mevcut şifre hatalı veya güncellenemedi.");
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Bir hata oluştu: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Kullanıcı adı güncelleme", 
        description = "Kullanıcı yeni bir kullanıcı adı belirler.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Kullanıcı adı güncellendi"),
        @ApiResponse(responseCode = "400", description = "Kullanıcı adı güncellenemedi"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim")
    })
    @PutMapping("/change-username")
    public ResponseEntity<String> changeUsername(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody UsernameUpdateRequest request
    ) {
        try {
            // Token kontrolü
            if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header is missing");
            }
            
            // Token'ı al (Bearer prefix'i varsa kaldır, yoksa direkt kullan)
            String token;
            if (authorizationHeader.startsWith("Bearer ")) {
                token = authorizationHeader.substring(7);
            } else {
                token = authorizationHeader.trim();
            }
            
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is invalid or expired");
            }
            
            Long userId = jwtProvider.getUserIdFromToken(token);
            boolean result = userService.changeUsername(userId, request.getNewUsername());
            
            if (result) {
                return ResponseEntity.ok("Kullanıcı adı başarıyla güncellendi.");
            } else {
                return ResponseEntity.badRequest().body("Kullanıcı adı güncellenemedi.");
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Bir hata oluştu: " + e.getMessage());
        }
    }

    @Operation(
        summary = "Kullanıcı silme", 
        description = "Admin kullanıcıları silebilir. Kullanıcı kendisini silemez, admin kendisini silemez.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Kullanıcı başarıyla silindi"),
        @ApiResponse(responseCode = "400", description = "Kullanıcı silinemedi"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim"),
        @ApiResponse(responseCode = "403", description = "Bu işlemi yapma yetkiniz yok"),
        @ApiResponse(responseCode = "404", description = "Kullanıcı bulunamadı")
    })
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<String> deleteUser(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long userId
    ) {
        try {
            // Token kontrolü
            if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header is missing");
            }
            
            // Token'ı al (Bearer prefix'i varsa kaldır, yoksa direkt kullan)
            String token;
            if (authorizationHeader.startsWith("Bearer ")) {
                token = authorizationHeader.substring(7);
            } else {
                token = authorizationHeader.trim();
            }
            
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is invalid or expired");
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            String currentUserRole = jwtProvider.getUserRoleFromToken(token);
            
            // Sadece ADMIN kullanıcı silebilir
            if (!currentUserRole.equals("ROLE_ADMIN")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bu işlemi yapma yetkiniz yok! Sadece admin kullanıcıları silebilir.");
            }
            
            // Kullanıcı kendisini silemesin
            if (currentUserId.equals(userId)) {
                return ResponseEntity.badRequest().body("Kendinizi silemezsiniz!");
            }
            
            boolean result = userService.deleteUser(currentUserId, userId);
            
            if (result) {
                return ResponseEntity.ok("Kullanıcı başarıyla silindi.");
            } else {
                return ResponseEntity.badRequest().body("Kullanıcı silinemedi. Kullanıcı bulunamadı veya yetkiniz yok.");
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Bir hata oluştu: " + e.getMessage());
        }
    }
}
