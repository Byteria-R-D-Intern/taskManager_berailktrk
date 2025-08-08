package com.berailktrk.taskManager.presentation.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
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

import com.berailktrk.taskManager.application.usecase.TaskService;
import com.berailktrk.taskManager.domain.model.Task;
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header is missing");
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is invalid or expired");
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            Task task = taskService.createTask(request, currentUserId);
            TaskResponse response = new TaskResponse(task);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            String message = e.getMessage();
            
            // Validation hataları
            if (message.contains("boş olamaz") || message.contains("uzun olamaz") || 
                message.contains("geçmiş bir tarih")) {
                return ResponseEntity.badRequest().body("Validation Error: " + message);
            }
            
            // Yetki hataları
            if (message.contains("yetkiniz yok") || message.contains("yapamazsınız")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Permission Error: " + message);
            }
            
            // Bulunamadı hataları
            if (message.contains("bulunamadı")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found: " + message);
            }
            
            // Genel hata
            return ResponseEntity.badRequest().body("Error: " + message);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server Error: " + e.getMessage());
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header is missing");
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is invalid or expired");
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            Task task = taskService.updateTask(taskId, request, currentUserId);
            TaskResponse response = new TaskResponse(task);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            String message = e.getMessage();
            
            // Validation hataları
            if (message.contains("boş olamaz") || message.contains("uzun olamaz") || 
                message.contains("geçmiş bir tarih")) {
                return ResponseEntity.badRequest().body("Validation Error: " + message);
            }
            
            // Yetki hataları
            if (message.contains("yetkiniz yok") || message.contains("yapamazsınız")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Permission Error: " + message);
            }
            
            // Bulunamadı hataları
            if (message.contains("bulunamadı")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found: " + message);
            }
            
            // Genel hata
            return ResponseEntity.badRequest().body("Error: " + message);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server Error: " + e.getMessage());
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header is missing");
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is invalid or expired");
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            boolean result = taskService.deleteTask(taskId, currentUserId);
            
            if (result) {
                return ResponseEntity.ok("Görev başarıyla silindi.");
            } else {
                return ResponseEntity.badRequest().body("Görev silinemedi.");
            }
            
        } catch (RuntimeException e) {
            String message = e.getMessage();
            
            // Yetki hataları
            if (message.contains("yetkiniz yok")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Permission Error: " + message);
            }
            
            // Bulunamadı hataları
            if (message.contains("bulunamadı")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found: " + message);
            }
            
            // Genel hata
            return ResponseEntity.badRequest().body("Error: " + message);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server Error: " + e.getMessage());
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header is missing");
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is invalid or expired");
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            Task task = taskService.getTaskById(taskId, currentUserId);
            TaskResponse response = new TaskResponse(task);
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            String message = e.getMessage();
            
            // Yetki hataları
            if (message.contains("yetkiniz yok")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Permission Error: " + message);
            }
            
            // Bulunamadı hataları
            if (message.contains("bulunamadı")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found: " + message);
            }
            
            // Genel hata
            return ResponseEntity.badRequest().body("Error: " + message);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server Error: " + e.getMessage());
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            List<Task> tasks = taskService.getUserTasks(currentUserId);
            List<TaskResponse> responses = tasks.stream()
                .map(TaskResponse::new)
                .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
            
        } catch (RuntimeException e) {
            String message = e.getMessage();
            
            // Bulunamadı hataları
            if (message.contains("bulunamadı")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
    public ResponseEntity<?> getAllTasks(
        @RequestHeader(value = "Authorization", required = false) String authorizationHeader
    ) {
        try {
            // Token kontrolü
            if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header is missing");
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is invalid or expired");
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            List<Task> tasks = taskService.getAllTasks(currentUserId);
            List<TaskResponse> responses = tasks.stream()
                .map(TaskResponse::new)
                .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
            
        } catch (RuntimeException e) {
            String message = e.getMessage();
            
            // Yetki hataları
            if (message.contains("yetkiniz yok")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Permission Error: " + message);
            }
            
            // Bulunamadı hataları
            if (message.contains("bulunamadı")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found: " + message);
            }
            
            // Genel hata
            return ResponseEntity.badRequest().body("Error: " + message);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server Error: " + e.getMessage());
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
    public ResponseEntity<?> searchTasks(
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
            
            Page<Task> tasks = taskService.searchTasks(searchRequest, currentUserId);
            Page<TaskResponse> responses = tasks.map(TaskResponse::new);
            return ResponseEntity.ok(responses);
            
        } catch (DataAccessException e) {
            // Database hataları
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Database Error: " + e.getMessage());
        } catch (RuntimeException e) {
            String message = e.getMessage();
            
            // Bulunamadı hataları
            if (message.contains("bulunamadı")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found: " + message);
            }
            
            // Yetki hataları
            if (message.contains("yetkiniz yok")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Permission Error: " + message);
            }
            
            // Genel hata
            return ResponseEntity.badRequest().body("Error: " + message);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Server Error: " + e.getMessage());
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            List<Task> tasks = taskService.quickSearch(searchTerm, currentUserId);
            List<TaskResponse> responses = tasks.stream()
                .map(TaskResponse::new)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            Map<String, Object> statisticsData = taskService.getTaskStatistics(currentUserId);
            
            @SuppressWarnings("unchecked")
            Map<TaskStatus, Long> statusCounts = (Map<TaskStatus, Long>) statisticsData.get("statusCounts");
            @SuppressWarnings("unchecked")
            Map<TaskPriority, Long> priorityCounts = (Map<TaskPriority, Long>) statisticsData.get("priorityCounts");
            
            TaskStatistics statistics = new TaskStatistics(statusCounts, priorityCounts);
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String token = extractToken(authorizationHeader);
            if (!jwtProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            Long currentUserId = jwtProvider.getUserIdFromToken(token);
            Page<Task> tasks = taskService.getUserTasksPaginated(currentUserId, page, size);
            Page<TaskResponse> responses = tasks.map(TaskResponse::new);
            
            return ResponseEntity.ok(responses);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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