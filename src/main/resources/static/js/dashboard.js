const API_BASE_URL = 'http://localhost:8080/api';

// Sayfa yüklendiğinde
window.addEventListener('load', function() {
    const token = localStorage.getItem('authToken');
    console.log('Dashboard yükleniyor...');
    
    if (!token) {
        console.log('Token bulunamadı, login sayfasına yönlendiriliyor...');
        window.location.href = 'login.html';
        return;
    }
    
    // Token'ı kontrol et (artık gerekli değil çünkü auth.js'de Bearer prefix'i ekleniyor)
    console.log('Token kontrol ediliyor...');
    
    loadUserProfile();
    loadTasks();
    loadStatistics();
});

// Kullanıcı profilini yükle
async function loadUserProfile() {
    try {
        console.log('Profil yükleniyor...');
        const token = localStorage.getItem('authToken');
        
        const response = await fetch(`${API_BASE_URL}/auth/profile`, {
            headers: {
                'Authorization': token
            }
        });
        
        console.log('Profil response status:', response.status);
        
        if (response.ok) {
            const profile = await response.json();
            console.log('Profil yüklendi:', profile.username);
            document.getElementById('username').textContent = profile.username;
        } else {
            const errorText = await response.text();
            console.error('Profil yüklenemedi:', response.status, errorText);
        }
    } catch (error) {
        console.error('Profil yüklenemedi:', error);
    }
}

// Görevleri yükle
async function loadTasks() {
    try {
        console.log('Görevler yükleniyor...');
        const token = localStorage.getItem('authToken');
        
        const response = await fetch(`${API_BASE_URL}/tasks/my-tasks`, {
            headers: {
                'Authorization': token
            }
        });
        
        console.log('Görevler response status:', response.status);
        
        if (response.ok) {
            const tasks = await response.json();
            console.log('Görevler yüklendi:', tasks.length, 'adet');
            displayTasks(tasks);
        } else {
            const errorText = await response.text();
            console.error('Görevler yüklenemedi:', response.status, errorText);
            showAlert('Görevler yüklenemedi: ' + response.status, 'danger');
        }
    } catch (error) {
        console.error('Bağlantı hatası:', error);
        showAlert('Bağlantı hatası: ' + error.message, 'danger');
    }
}

// Görevleri görüntüle
function displayTasks(tasks) {
    const container = document.getElementById('tasksContainer');
    
    if (tasks.length === 0) {
        container.innerHTML = `
            <div class="text-center py-4">
                <i class="fas fa-inbox fa-3x text-muted mb-3"></i>
                <p class="text-muted">Henüz görev bulunmuyor.</p>
                <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addTaskModal">
                    <i class="fas fa-plus"></i> İlk Görevi Ekle
                </button>
            </div>
        `;
        return;
    }
    
    let html = '';
    tasks.forEach(task => {
        const priorityClass = getPriorityClass(task.priority);
        const statusClass = getStatusClass(task.status);
        
        html += `
            <div class="card task-card ${priorityClass} mb-3">
                <div class="card-body">
                    <div class="row align-items-center">
                        <div class="col-md-8">
                            <h6 class="card-title mb-1">${task.title}</h6>
                            <p class="card-text text-muted mb-2">${task.description || 'Açıklama yok'}</p>
                            <div class="d-flex gap-2">
                                <span class="task-status ${statusClass}">${getStatusText(task.status)}</span>
                                <span class="badge bg-${getPriorityColor(task.priority)}">${getPriorityText(task.priority)}</span>
                            </div>
                        </div>
                        <div class="col-md-4 text-end">
                            <button class="btn btn-sm btn-outline-primary me-1" onclick="editTask(${task.id})">
                                <i class="fas fa-edit"></i>
                            </button>
                            <button class="btn btn-sm btn-outline-danger" onclick="deleteTask(${task.id})">
                                <i class="fas fa-trash"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    });
    
    container.innerHTML = html;
}

// Görev ekle
async function addTask() {
    const title = document.getElementById('taskTitle').value;
    const description = document.getElementById('taskDescription').value;
    const priority = document.getElementById('taskPriority').value;
    const status = document.getElementById('taskStatus').value;
    
    try {
        const response = await fetch(`${API_BASE_URL}/tasks`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': localStorage.getItem('authToken')
            },
            body: JSON.stringify({
                title: title,
                description: description,
                priority: priority,
                status: status
            })
        });
        
        if (response.ok) {
            showAlert('Görev başarıyla eklendi!', 'success');
            document.getElementById('addTaskForm').reset();
            bootstrap.Modal.getInstance(document.getElementById('addTaskModal')).hide();
            loadTasks();
            loadStatistics();
        } else {
            const error = await response.text();
            showAlert('Görev eklenemedi: ' + error, 'danger');
        }
    } catch (error) {
        showAlert('Bağlantı hatası: ' + error.message, 'danger');
    }
}

// İstatistikleri yükle
async function loadStatistics() {
    try {
        console.log('İstatistikler yükleniyor...');
        const token = localStorage.getItem('authToken');
        
        const response = await fetch(`${API_BASE_URL}/tasks/statistics`, {
            headers: {
                'Authorization': token
            }
        });
        
        console.log('İstatistik response status:', response.status);
        
        if (response.ok) {
            const stats = await response.json();
            console.log('İstatistikler yüklendi');
            document.getElementById('totalTasks').textContent = stats.totalTasks || 0;
            document.getElementById('pendingTasks').textContent = stats.pendingTasks || 0;
            document.getElementById('inProgressTasks').textContent = stats.inProgressTasks || 0;
            document.getElementById('completedTasks').textContent = stats.completedTasks || 0;
        } else {
            const errorText = await response.text();
            console.error('İstatistikler yüklenemedi:', response.status, errorText);
        }
    } catch (error) {
        console.error('İstatistikler yüklenemedi:', error);
    }
}

// Yardımcı fonksiyonlar
function getPriorityClass(priority) {
    switch(priority) {
        case 'HIGH': return 'high-priority';
        case 'MEDIUM': return 'medium-priority';
        case 'LOW': return 'low-priority';
        default: return '';
    }
}

function getPriorityColor(priority) {
    switch(priority) {
        case 'HIGH': return 'danger';
        case 'MEDIUM': return 'warning';
        case 'LOW': return 'success';
        default: return 'secondary';
    }
}

function getPriorityText(priority) {
    switch(priority) {
        case 'HIGH': return 'Yüksek';
        case 'MEDIUM': return 'Orta';
        case 'LOW': return 'Düşük';
        default: return priority;
    }
}

function getStatusClass(status) {
    switch(status) {
        case 'PENDING': return 'status-pending';
        case 'IN_PROGRESS': return 'status-in-progress';
        case 'COMPLETED': return 'status-completed';
        default: return '';
    }
}

function getStatusText(status) {
    switch(status) {
        case 'PENDING': return 'Bekleyen';
        case 'IN_PROGRESS': return 'Devam Eden';
        case 'COMPLETED': return 'Tamamlanan';
        default: return status;
    }
}

function showAlert(message, type) {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
    alertDiv.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.body.appendChild(alertDiv);
    
    setTimeout(() => {
        if (alertDiv.parentNode) {
            alertDiv.remove();
        }
    }, 5000);
}

function logout() {
    localStorage.removeItem('authToken');
    window.location.href = 'login.html';
}

function searchTasks() {
    const searchTerm = document.getElementById('searchInput').value;
    // Arama fonksiyonu implement edilebilir
    console.log('Arama:', searchTerm);
}

function filterTasks() {
    const status = document.getElementById('statusFilter').value;
    // Filtreleme fonksiyonu implement edilebilir
    console.log('Filtre:', status);
} 