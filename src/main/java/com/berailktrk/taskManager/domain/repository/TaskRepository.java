package com.berailktrk.taskManager.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.berailktrk.taskManager.domain.model.Task;
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
} 