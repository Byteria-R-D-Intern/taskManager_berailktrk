package com.berailktrk.taskManager.application.usecase;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.berailktrk.taskManager.domain.model.Role;
import com.berailktrk.taskManager.domain.model.Task;
import com.berailktrk.taskManager.domain.model.TaskPriority;
import com.berailktrk.taskManager.domain.model.TaskStatus;
import com.berailktrk.taskManager.domain.model.User;
import com.berailktrk.taskManager.domain.repository.TaskRepository;
import com.berailktrk.taskManager.domain.repository.UserRepository;
import com.berailktrk.taskManager.presentation.dto.TaskRequest;
import com.berailktrk.taskManager.presentation.dto.TaskSearchRequest;

@Service
public class TaskService {
    
    @Autowired
    private TaskRepository taskRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // Business Logic: Görev oluşturma
    public Task createTask(TaskRequest request, Long currentUserId) {
        // 1. Kullanıcı kontrolü
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        
        // 2. Veri doğrulama
        validateTaskRequest(request);
        
        // 3. Yetki kontrolü - URGENT öncelik sadece ADMIN için
        if (request.getPriority() == TaskPriority.URGENT && 
            !currentUser.getRole().equals(Role.ROLE_ADMIN)) {
            throw new RuntimeException("URGENT öncelikli görev oluşturma yetkiniz yok");
        }
        
        // 4. Görev atama kontrolü
        User assignedUser = null;
        if (request.getAssignedToUserId() != null) {
            assignedUser = userRepository.findById(request.getAssignedToUserId())
                .orElseThrow(() -> new RuntimeException("Atanacak kullanıcı bulunamadı"));
            
            // Kendine görev atama kontrolü
            if (assignedUser.getId().equals(currentUserId)) {
                throw new RuntimeException("Kendinize görev atayamazsınız");
            }
            
            // Atama yetkisi kontrolü
            if (!canAssignTask(currentUser.getRole())) {
                throw new RuntimeException("Görev atama yetkiniz yok");
            }
        }
        
        // 5. Görev oluşturma
        Task task = new Task(request.getTitle(), request.getDescription(), currentUser);
        task.setStatus(request.getStatus() != null ? request.getStatus() : TaskStatus.PENDING);
        task.setPriority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM);
        task.setAssignedTo(assignedUser);
        task.setDueDate(request.getDueDate());
        
        return taskRepository.save(task);
    }
    
    // Business Logic: Görev güncelleme
    public Task updateTask(Long taskId, TaskRequest request, Long currentUserId) {
        // 1. Görev kontrolü
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Görev bulunamadı"));
        
        // 2. Sahiplik kontrolü
        if (!canUpdateTask(task, currentUserId)) {
            throw new RuntimeException("Bu görevi güncelleme yetkiniz yok");
        }
        
        // 3. Veri doğrulama
        validateTaskRequest(request);
        
        // 4. Durum geçiş kontrolü
        if (request.getStatus() != null && !canChangeStatus(task, request.getStatus(), currentUserId)) {
            throw new RuntimeException("Bu duruma geçiş yapamazsınız");
        }
        
        // 5. Görev atama kontrolü
        if (request.getAssignedToUserId() != null) {
            User assignedUser = userRepository.findById(request.getAssignedToUserId())
                .orElseThrow(() -> new RuntimeException("Atanacak kullanıcı bulunamadı"));
            
            if (!canAssignTask(getCurrentUserRole(currentUserId))) {
                throw new RuntimeException("Görev atama yetkiniz yok");
            }
            
            task.setAssignedTo(assignedUser);
        }
        
        // 6. Görev güncelleme
        if (request.getTitle() != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getStatus() != null) task.setStatus(request.getStatus());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());
        
        return taskRepository.save(task);
    }
    
    // Business Logic: Görev silme
    public boolean deleteTask(Long taskId, Long currentUserId) {
        // 1. Görev kontrolü
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Görev bulunamadı"));
        
        // 2. Sahiplik kontrolü
        if (!canDeleteTask(task, currentUserId)) {
            throw new RuntimeException("Bu görevi silme yetkiniz yok");
        }
        
        // 3. Görev silme
        taskRepository.delete(task);
        return true;
    }
    
    // Business Logic: Görev detayı getirme
    public Task getTaskById(Long taskId, Long currentUserId) {
        // 1. Görev kontrolü
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new RuntimeException("Görev bulunamadı"));
        
        // 2. Görüntüleme yetkisi kontrolü
        if (!canViewTask(task, currentUserId)) {
            throw new RuntimeException("Bu görevi görme yetkiniz yok");
        }
        
        return task;
    }
    
    // Business Logic: Kullanıcının görevlerini listeleme
    public List<Task> getUserTasks(Long currentUserId) {
        return taskRepository.findByCreatedByOrAssignedToUserId(currentUserId);
    }
    
    // Business Logic: Tüm görevleri listeleme (Admin/Manager)
    public List<Task> getAllTasks(Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        
        // Sadece ADMIN ve MANAGER tüm görevleri görebilir
        if (!currentUser.getRole().equals(Role.ROLE_ADMIN) && 
            !currentUser.getRole().equals(Role.ROLE_MANAGER)) {
            throw new RuntimeException("Tüm görevleri görme yetkiniz yok");
        }
        
        return taskRepository.findAll();
    }
    
    // Business Logic: Görev arama ve filtreleme
    public Page<Task> searchTasks(TaskSearchRequest searchRequest, Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        
        // Sayfalama ve sıralama ayarları
        Sort sort = Sort.by(Sort.Direction.fromString(searchRequest.getSortDirection()), searchRequest.getSortBy());
        Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);
        
        Page<Task> tasks;
        
        // Admin/Manager tüm görevleri görebilir, diğer kullanıcılar sadece kendi görevlerini
        if (currentUser.getRole().equals(Role.ROLE_ADMIN) || currentUser.getRole().equals(Role.ROLE_MANAGER)) {
            tasks = taskRepository.findAllTasksWithFilters(
                searchRequest.getTitle(), 
                searchRequest.getStatus(), 
                searchRequest.getPriority(), 
                pageable
            );
        } else {
            tasks = taskRepository.findTasksWithFilters(
                searchRequest.getTitle(), 
                searchRequest.getStatus(), 
                searchRequest.getPriority(), 
                currentUserId,
                pageable
            );
        }
        
        return tasks;
    }
    
    // Business Logic: Kullanıcının görevlerini sayfalama ile getirme
    public Page<Task> getUserTasksPaginated(Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return taskRepository.findByCreatedByIdOrAssignedToId(currentUserId, pageable);
    }
    
    // Business Logic: Görev istatistikleri
    public Map<String, Object> getTaskStatistics(Long currentUserId) {
        List<Task> tasks = getUserTasks(currentUserId);
        
        // Durum bazında sayım
        Map<TaskStatus, Long> statusMap = tasks.stream()
            .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
        
        // Öncelik bazında sayım
        Map<TaskPriority, Long> priorityMap = tasks.stream()
            .collect(Collectors.groupingBy(Task::getPriority, Collectors.counting()));
        
        return Map.of("statusCounts", statusMap, "priorityCounts", priorityMap);
    }
    
    // Business Logic: Hızlı arama
    public List<Task> quickSearch(String searchTerm, Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        
        List<Task> tasks;
        
        // Admin/Manager tüm görevlerde arama yapabilir
        if (currentUser.getRole().equals(Role.ROLE_ADMIN) || currentUser.getRole().equals(Role.ROLE_MANAGER)) {
            tasks = taskRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(searchTerm);
        } else {
            // Diğer kullanıcılar sadece kendi görevlerinde arama yapabilir
            tasks = taskRepository.findByCreatedByOrAssignedToUserIdAndTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(currentUserId, searchTerm);
        }
        
        return tasks;
    }
    
    // Helper Methods - Business Logic Rules
    
    private void validateTaskRequest(TaskRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new RuntimeException("Görev başlığı boş olamaz");
        }
        
        if (request.getTitle().length() > 100) {
            throw new RuntimeException("Görev başlığı 100 karakterden uzun olamaz");
        }
        
        if (request.getDescription() != null && request.getDescription().length() > 500) {
            throw new RuntimeException("Görev açıklaması 500 karakterden uzun olamaz");
        }
        
        if (request.getDueDate() != null && request.getDueDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Bitiş tarihi geçmiş bir tarih olamaz");
        }
    }
    
    private boolean canAssignTask(Role userRole) {
        return userRole.equals(Role.ROLE_ADMIN) || userRole.equals(Role.ROLE_MANAGER);
    }
    
    private boolean canUpdateTask(Task task, Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        
        // Görevi oluşturan kişi güncelleyebilir
        if (task.getCreatedBy().getId().equals(currentUserId)) {
            return true;
        }
        
        // Göreve atanan kişi güncelleyebilir
        if (task.getAssignedTo() != null && task.getAssignedTo().getId().equals(currentUserId)) {
            return true;
        }
        
        // ADMIN her görevi güncelleyebilir
        return currentUser.getRole().equals(Role.ROLE_ADMIN);
    }
    
    private boolean canDeleteTask(Task task, Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        
        // Görevi oluşturan kişi silebilir
        if (task.getCreatedBy().getId().equals(currentUserId)) {
            return true;
        }
        
        // ADMIN her görevi silebilir
        return currentUser.getRole().equals(Role.ROLE_ADMIN);
    }
    
    private boolean canViewTask(Task task, Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        
        // Görevi oluşturan kişi görebilir
        if (task.getCreatedBy().getId().equals(currentUserId)) {
            return true;
        }
        
        // Göreve atanan kişi görebilir
        if (task.getAssignedTo() != null && task.getAssignedTo().getId().equals(currentUserId)) {
            return true;
        }
        
        // ADMIN/MANAGER her görevi görebilir
        return currentUser.getRole().equals(Role.ROLE_ADMIN) || 
               currentUser.getRole().equals(Role.ROLE_MANAGER);
    }
    
    private boolean canChangeStatus(Task task, TaskStatus newStatus, Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        
        // ADMIN her duruma geçiş yapabilir
        if (currentUser.getRole().equals(Role.ROLE_ADMIN)) {
            return true;
        }
        
        // Görevi oluşturan veya atanan kişi durumu değiştirebilir
        if (task.getCreatedBy().getId().equals(currentUserId) || 
            (task.getAssignedTo() != null && task.getAssignedTo().getId().equals(currentUserId))) {
            return true;
        }
        
        return false;
    }
    
    private Role getCurrentUserRole(Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        return currentUser.getRole();
    }
} 