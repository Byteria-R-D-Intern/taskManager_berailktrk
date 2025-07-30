package com.berailktrk.taskManager.domain.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.berailktrk.taskManager.domain.model.Task;
import com.berailktrk.taskManager.domain.model.TaskPriority;
import com.berailktrk.taskManager.domain.model.TaskStatus;
import com.berailktrk.taskManager.domain.model.User;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    
    // Kullanıcının oluşturduğu görevler
    List<Task> findByCreatedBy(User createdBy);
    
    // Kullanıcıya atanan görevler
    List<Task> findByAssignedTo(User assignedTo);
    
    // Belirli bir durumdaki görevler
    List<Task> findByStatus(TaskStatus status);
    
    // Kullanıcının oluşturduğu ve belirli durumdaki görevler
    List<Task> findByCreatedByAndStatus(User createdBy, TaskStatus status);
    
    // Kullanıcıya atanan ve belirli durumdaki görevler
    List<Task> findByAssignedToAndStatus(User assignedTo, TaskStatus status);
    
    // Başlığa göre arama (case-insensitive)
    @Query("SELECT t FROM Task t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Task> findByTitleContainingIgnoreCase(@Param("title") String title);
    
    // Açıklamaya göre arama (case-insensitive)
    @Query("SELECT t FROM Task t WHERE LOWER(t.description) LIKE LOWER(CONCAT('%', :description, '%'))")
    List<Task> findByDescriptionContainingIgnoreCase(@Param("description") String description);
    
    // Kullanıcının oluşturduğu veya atandığı görevler
    @Query("SELECT t FROM Task t WHERE t.createdBy = :user OR t.assignedTo = :user")
    List<Task> findByCreatedByOrAssignedTo(@Param("user") User user);
    
    // Belirli bir kullanıcının oluşturduğu veya atandığı görevler
    @Query("SELECT t FROM Task t WHERE (t.createdBy.id = :userId OR t.assignedTo.id = :userId)")
    List<Task> findByCreatedByOrAssignedToUserId(@Param("userId") Long userId);
    
    // ========== YENİ ARAMA VE FİLTRELEME METHODLARI ==========
    
    // Kullanıcının görevlerini filtreleme (başlık, durum, öncelik)
    @Query("SELECT t FROM Task t WHERE " +
           "(:title IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:priority IS NULL OR t.priority = :priority) AND " +
           "(t.createdBy.id = :currentUserId OR t.assignedTo.id = :currentUserId)")
    Page<Task> findTasksWithFilters(
        @Param("title") String title,
        @Param("status") TaskStatus status,
        @Param("priority") TaskPriority priority,
        @Param("currentUserId") Long currentUserId,
        Pageable pageable
    );
    
    // Admin/Manager için tüm görevleri filtreleme
    @Query("SELECT t FROM Task t WHERE " +
           "(:title IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:priority IS NULL OR t.priority = :priority)")
    Page<Task> findAllTasksWithFilters(
        @Param("title") String title,
        @Param("status") TaskStatus status,
        @Param("priority") TaskPriority priority,
        Pageable pageable
    );
    
    // Kullanıcının görevlerini sayfalama ile getirme
    @Query("SELECT t FROM Task t WHERE t.createdBy.id = :userId OR t.assignedTo.id = :userId")
    Page<Task> findByCreatedByIdOrAssignedToId(@Param("userId") Long userId, Pageable pageable);
    
    // Durum bazlı görev sayıları
    @Query("SELECT t.status, COUNT(t) FROM Task t WHERE " +
           "(t.createdBy.id = :userId OR t.assignedTo.id = :userId) " +
           "GROUP BY t.status")
    List<Object[]> getTaskCountsByStatus(@Param("userId") Long userId);
    
    // Öncelik bazlı görev sayıları
    @Query("SELECT t.priority, COUNT(t) FROM Task t WHERE " +
           "(t.createdBy.id = :userId OR t.assignedTo.id = :userId) " +
           "GROUP BY t.priority")
    List<Object[]> getTaskCountsByPriority(@Param("userId") Long userId);
    
    // Hızlı arama - kullanıcının görevlerinde başlık veya açıklama arama
    @Query("SELECT t FROM Task t WHERE " +
           "(t.createdBy.id = :userId OR t.assignedTo.id = :userId) AND " +
           "(LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Task> findByCreatedByOrAssignedToUserIdAndTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
        @Param("userId") Long userId,
        @Param("searchTerm") String searchTerm
    );
    
    // Admin/Manager için tüm görevlerde başlık veya açıklama arama
    @Query("SELECT t FROM Task t WHERE " +
           "LOWER(t.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Task> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
        @Param("searchTerm") String searchTerm
    );
}