// JavaScript Document
const API_BASE_URL = 'http://localhost:8080/api';

// Login form submit
document.getElementById('loginForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const loginBtn = document.getElementById('loginBtn');
    const btnText = loginBtn.querySelector('.btn-text');
    const loading = loginBtn.querySelector('.loading');
    
    // Loading durumunu göster
    btnText.style.display = 'none';
    loading.style.display = 'inline-block';
    loginBtn.disabled = true;
    
    try {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                username: username,
                password: password
            })
        });
        
        if (response.ok) {
            const token = await response.text();
            
            // Token'ı localStorage'a kaydet (Bearer prefix'i ile)
            localStorage.setItem('authToken', `Bearer ${token}`);
            
            // Başarı mesajı göster
            showAlert('Giriş başarılı! Yönlendiriliyorsunuz...', 'success');
            
            // Dashboard'a yönlendir
            setTimeout(() => {
                window.location.href = 'dashboard.html';
            }, 1500);
            
        } else {
            const errorText = await response.text();
            showAlert('Giriş başarısız: ' + errorText, 'danger');
        }
        
    } catch (error) {
        showAlert('Bağlantı hatası: ' + error.message, 'danger');
    } finally {
        // Loading durumunu gizle
        btnText.style.display = 'inline';
        loading.style.display = 'none';
        loginBtn.disabled = false;
    }
});

// Alert gösterme fonksiyonu
function showAlert(message, type) {
    const alertContainer = document.getElementById('alert-container');
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
    alertDiv.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    alertContainer.appendChild(alertDiv);
    
    // 5 saniye sonra otomatik kaldır
    setTimeout(() => {
        if (alertDiv.parentNode) {
            alertDiv.remove();
        }
    }, 5000);
}

// Sayfa yüklendiğinde token kontrolü
window.addEventListener('load', function() {
    const token = localStorage.getItem('authToken');
    if (token) {
        // Zaten giriş yapmışsa dashboard'a yönlendir
        window.location.href = 'dashboard.html';
    }
});