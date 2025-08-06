// JavaScript Document
const API_BASE_URL = 'http://localhost:8080/api';

// Register form submit
document.getElementById('registerForm').addEventListener('submit', async function(e) {
    e.preventDefault();
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    const termsCheck = document.getElementById('termsCheck').checked;
    const registerBtn = document.getElementById('registerBtn');
    const btnText = registerBtn.querySelector('.btn-text');
    const loading = registerBtn.querySelector('.loading');
    
    // Validasyonlar
    if (password !== confirmPassword) {
        showAlert('Şifreler eşleşmiyor!', 'danger');
        return;
    }
    
    if (!termsCheck) {
        showAlert('Kullanım şartlarını kabul etmelisiniz!', 'danger');
        return;
    }
    
    // Loading durumunu göster
    btnText.style.display = 'none';
    loading.style.display = 'inline-block';
    registerBtn.disabled = true;
    
    try {
        const response = await fetch(`${API_BASE_URL}/auth/register`, {
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
            const result = await response.text();
            showAlert('Kayıt başarılı! Giriş sayfasına yönlendiriliyorsunuz...', 'success');
            
            // Login sayfasına yönlendir
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 2000);
            
        } else {
            const errorText = await response.text();
            showAlert('Kayıt başarısız: ' + errorText, 'danger');
        }
        
    } catch (error) {
        showAlert('Bağlantı hatası: ' + error.message, 'danger');
    } finally {
        // Loading durumunu gizle
        btnText.style.display = 'inline';
        loading.style.display = 'none';
        registerBtn.disabled = false;
    }
});

// Şifre güçlülük kontrolü
document.getElementById('password').addEventListener('input', function() {
    const password = this.value;
    const strengthDiv = document.getElementById('passwordStrength');
    
    let strength = 0;
    let feedback = '';
    
    if (password.length >= 8) strength++;
    if (/[a-z]/.test(password)) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[0-9]/.test(password)) strength++;
    if (/[^A-Za-z0-9]/.test(password)) strength++;
    
    switch(strength) {
        case 0:
        case 1:
            feedback = '<span class="text-danger">Çok zayıf</span>';
            break;
        case 2:
            feedback = '<span class="text-warning">Zayıf</span>';
            break;
        case 3:
            feedback = '<span class="text-info">Orta</span>';
            break;
        case 4:
            feedback = '<span class="text-primary">Güçlü</span>';
            break;
        case 5:
            feedback = '<span class="text-success">Çok güçlü</span>';
            break;
    }
    
    strengthDiv.innerHTML = feedback;
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