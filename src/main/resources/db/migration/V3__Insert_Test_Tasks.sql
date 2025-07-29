-- Test görevleri ekle (kullanıcı ID'leri 1, 2, 3 olmalı)
INSERT INTO tasks (title, description, status, priority, assigned_to, created_by, due_date) VALUES
-- Admin (ID: 1) tarafından oluşturulan görevler
('Proje Planlaması', 'Yeni proje için detaylı planlama yapılması gerekiyor', 'PENDING', 'HIGH', 2, 1, '2024-03-20 17:00:00'),
('Database Tasarımı', 'Veritabanı şemasının tasarlanması ve implementasyonu', 'IN_PROGRESS', 'URGENT', 3, 1, '2024-03-15 14:00:00'),
('API Dokümantasyonu', 'REST API endpoint\'lerinin dokümante edilmesi', 'COMPLETED', 'MEDIUM', 2, 1, '2024-03-10 16:00:00'),

-- Manager (ID: 2) tarafından oluşturulan görevler
('Frontend Geliştirme', 'React uygulamasının geliştirilmesi', 'PENDING', 'HIGH', 3, 2, '2024-03-25 18:00:00'),
('Test Yazımı', 'Unit testlerin yazılması ve test coverage artırılması', 'IN_PROGRESS', 'MEDIUM', 1, 2, '2024-03-18 15:00:00'),
('Code Review', 'Pull request\'lerin review edilmesi', 'PENDING', 'LOW', 1, 2, '2024-03-22 12:00:00'),

-- User (ID: 3) tarafından oluşturulan görevler
('Bug Fix', 'Kritik bug\'ın düzeltilmesi', 'PENDING', 'URGENT', 1, 3, '2024-03-12 10:00:00'),
('Dokümantasyon Güncelleme', 'README dosyasının güncellenmesi', 'COMPLETED', 'LOW', 3, 3, '2024-03-08 11:00:00'),
('Performance Optimizasyonu', 'Uygulama performansının iyileştirilmesi', 'IN_PROGRESS', 'HIGH', 2, 3, '2024-03-30 17:00:00'); 