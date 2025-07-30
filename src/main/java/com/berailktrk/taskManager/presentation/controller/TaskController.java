package com.berailktrk.taskManager.presentation.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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

import com.berailktrk.taskManager.application.usecase.TaskService;
import com.berailktrk.taskManager.domain.model.TaskPriority;
import com.berailktrk.taskManager.domain.model.TaskStatus;
import com.berailktrk.taskManager.infrastructure.security.JwtProvider;
import com.berailktrk.taskManager.presentation.dto.TaskRequest;
import com.berailktrk.taskManager.presentation.dto.TaskResponse;
import com.berailktrk.taskManager.presentation.dto.TaskSearchRequest;
import com.berailktrk.taskManager.presentation.dto.TaskStatistics;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Task Management", description = "Görev yönetimi endpoint'leri")
public class TaskController {
    
    @Autowired
    private TaskService taskService;
    
    @Autowired
    private JwtProvider jwtProvider;
    
    @Operation(
        summary = "Görev oluşturma", 
        description = "Yeni bir görev oluşturur. URGENT öncelik sadece ADMIN için.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Görev başarıyla oluşturuldu"),
        @ApiResponse(responseCode = "400", description = "Geçersiz veri"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim"),
        @ApiResponse(responseCode = "403", description = "Bu işlemi yapma yetkiniz yok"),
        @ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    @PostMapping
    public ResponseEntity<?> createTask(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestBody TaskRequest request
    ) {
        try {
            // Token kontrolü
            if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
                return ResponseEntity.status(401).body("Authorization header is missing");
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(401).body("Token is invalid or expired");
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            TaskResponse response = taskService.createTask(request, currentUserId);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Hata: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Sunucu hatası: " + e.getMessage());
        }
    }
    
    @Operation(
        summary = "Görev güncelleme", 
        description = "Mevcut bir görevi günceller. Sadece görevi oluşturan, atanan veya ADMIN güncelleyebilir.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Görev başarıyla güncellendi"),
        @ApiResponse(responseCode = "400", description = "Geçersiz veri"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim"),
        @ApiResponse(responseCode = "403", description = "Bu görevi güncelleme yetkiniz yok"),
        @ApiResponse(responseCode = "404", description = "Görev bulunamadı"),
        @ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    @PutMapping("/{taskId}")
    public ResponseEntity<?> updateTask(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long taskId,
        @RequestBody TaskRequest request
    ) {
        try {
            // Token kontrolü
            if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
                return ResponseEntity.status(401).body("Authorization header is missing");
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(401).body("Token is invalid or expired");
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            TaskResponse response = taskService.updateTask(taskId, request, currentUserId);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Hata: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Sunucu hatası: " + e.getMessage());
        }
    }
    
    @Operation(
        summary = "Görev silme", 
        description = "Bir görevi siler. Sadece görevi oluşturan veya ADMIN silebilir.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Görev başarıyla silindi"),
        @ApiResponse(responseCode = "400", description = "Geçersiz veri"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim"),
        @ApiResponse(responseCode = "403", description = "Bu görevi silme yetkiniz yok"),
        @ApiResponse(responseCode = "404", description = "Görev bulunamadı"),
        @ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    @DeleteMapping("/{taskId}")
    public ResponseEntity<String> deleteTask(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long taskId
    ) {
        try {
            // Token kontrolü
            if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
                return ResponseEntity.status(401).body("Authorization header is missing");
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(401).body("Token is invalid or expired");
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            boolean result = taskService.deleteTask(taskId, currentUserId);
            
            if (result) {
                return ResponseEntity.ok("Görev başarıyla silindi.");
            } else {
                return ResponseEntity.badRequest().body("Görev silinemedi.");
            }
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Bir hata oluştu: " + e.getMessage());
        }
    }
    
    @Operation(
        summary = "Görev detayı getirme", 
        description = "Belirli bir görevin detaylarını getirir.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Görev detayları"),
        @ApiResponse(responseCode = "400", description = "Geçersiz istek"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim"),
        @ApiResponse(responseCode = "403", description = "Bu görevi görme yetkiniz yok"),
        @ApiResponse(responseCode = "404", description = "Görev bulunamadı"),
        @ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTaskById(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @PathVariable Long taskId
    ) {
        try {
            // Token kontrolü
            if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
                return ResponseEntity.status(401).body("Authorization header is missing");
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(401).body("Token is invalid or expired");
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            TaskResponse response = taskService.getTaskById(taskId, currentUserId);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Hata: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Sunucu hatası: " + e.getMessage());
        }
    }
    
    @Operation(
        summary = "Kullanıcının görevlerini listeleme", 
        description = "Kullanıcının oluşturduğu veya atandığı görevleri listeler.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Görev listesi"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim"),
        @ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    @GetMapping("/my-tasks")
    public ResponseEntity<List<TaskResponse>> getMyTasks(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            // Token kontrolü
            if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
                return ResponseEntity.status(401).build();
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(401).build();
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            List<TaskResponse> tasks = taskService.getUserTasks(currentUserId);
            return ResponseEntity.ok(tasks);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    @Operation(
        summary = "Tüm görevleri listeleme", 
        description = "Tüm görevleri listeler. Sadece ADMIN ve MANAGER erişebilir.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tüm görevler"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim"),
        @ApiResponse(responseCode = "403", description = "Bu işlemi yapma yetkiniz yok"),
        @ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            // Token kontrolü
            if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
                return ResponseEntity.status(401).build();
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(401).build();
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            List<TaskResponse> tasks = taskService.getAllTasks(currentUserId);
            return ResponseEntity.ok(tasks);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    // ========== YENİ ARAMA VE FİLTRELEME ENDPOINT'LERİ ==========
    
    @Operation(
        summary = "Görev arama ve filtreleme",
        description = "Görevleri başlık, durum, öncelik gibi kriterlere göre arar ve filtreler. " +
                     "Admin/Manager tüm görevleri, diğer kullanıcılar sadece kendi görevlerini görebilir.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Filtrelenmiş görev listesi"),
        @ApiResponse(responseCode = "400", description = "Geçersiz filtre parametreleri"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim"),
        @ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    @GetMapping("/search")
    public ResponseEntity<Page<TaskResponse>> searchTasks(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(required = false) String title,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String priority,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createdAt") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        try {
            if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
                return ResponseEntity.status(401).build();
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(401).build();
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            
            // TaskSearchRequest oluştur
            TaskSearchRequest searchRequest = new TaskSearchRequest();
            searchRequest.setTitle(title);
            
            // Status enum'a çevir
            if (status != null && !status.trim().isEmpty()) {
                try {
                    searchRequest.setStatus(TaskStatus.valueOf(status.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().build();
                }
            }
            
            // Priority enum'a çevir
            if (priority != null && !priority.trim().isEmpty()) {
                try {
                    searchRequest.setPriority(TaskPriority.valueOf(priority.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().build();
                }
            }
            
            searchRequest.setPage(page);
            searchRequest.setSize(size);
            searchRequest.setSortBy(sortBy);
            searchRequest.setSortDirection(sortDirection);
            
            Page<TaskResponse> tasks = taskService.searchTasks(searchRequest, currentUserId);
            return ResponseEntity.ok(tasks);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    @Operation(
        summary = "Hızlı arama",
        description = "Görev başlığı ve açıklamasında arama yapar.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Arama sonuçları"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim"),
        @ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    @GetMapping("/quick-search")
    public ResponseEntity<List<TaskResponse>> quickSearch(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam String searchTerm
    ) {
        try {
            if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
                return ResponseEntity.status(401).build();
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(401).build();
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            List<TaskResponse> tasks = taskService.quickSearch(searchTerm, currentUserId);
            
            return ResponseEntity.ok(tasks);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    @Operation(
        summary = "Görev istatistikleri",
        description = "Kullanıcının görevlerinin durum ve öncelik bazında istatistiklerini getirir.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Görev istatistikleri"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim"),
        @ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    @GetMapping("/statistics")
    public ResponseEntity<TaskStatistics> getTaskStatistics(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
                return ResponseEntity.status(401).build();
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(401).build();
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            TaskStatistics statistics = taskService.getTaskStatistics(currentUserId);
            
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    @Operation(
        summary = "Kullanıcının görevlerini sayfalama ile getirme",
        description = "Kullanıcının görevlerini sayfalama ile getirir.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Görev listesi"),
        @ApiResponse(responseCode = "401", description = "Yetkisiz erişim"),
        @ApiResponse(responseCode = "500", description = "Sunucu hatası")
    })
    @GetMapping("/my-tasks-paginated")
    public ResponseEntity<Page<TaskResponse>> getMyTasksPaginated(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        try {
            if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
                return ResponseEntity.status(401).build();
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(401).build();
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            Page<TaskResponse> tasks = taskService.getUserTasksPaginated(currentUserId, page, size);
            
            return ResponseEntity.ok(tasks);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
    
    // Helper method to extract token
    private String extractToken(String authorizationHeader) {
        if (authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        } else {
            return authorizationHeader.trim();
        }
    }
} 