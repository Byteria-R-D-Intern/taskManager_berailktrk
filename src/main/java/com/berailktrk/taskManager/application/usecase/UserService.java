package com.berailktrk.taskManager.application.usecase;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.berailktrk.taskManager.domain.model.Role;
import com.berailktrk.taskManager.domain.model.User;
import com.berailktrk.taskManager.domain.model.UserDetails;
import com.berailktrk.taskManager.domain.repository.UserDetailsRepository;
import com.berailktrk.taskManager.domain.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsRepository userDetailsRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserDetailsRepository userDetailsRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsRepository = userDetailsRepository;
    }

    public User register(String username, String password, Role role) {
        String encodedPassword = passwordEncoder.encode(password);
        User user = new User(null, username, encodedPassword, role);
        return userRepository.save(user);
    }
    public Optional<User> authenticate(String username, String rawPassword) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (passwordEncoder.matches(rawPassword, user.getPassword())) {
                    return Optional.of(user);
                }
            }
            return Optional.empty();
        }
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> updateUser(Long id, String newUsername, String newPassword) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (newUsername != null && !newUsername.isBlank()) {
                user.setUsername(newUsername);
            }
            if (newPassword != null && !newPassword.isBlank()) {
                user.setPassword(passwordEncoder.encode(newPassword));
            }
            userRepository.save(user);
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<UserDetails> getUserProfile(Long userId) {
        return userDetailsRepository.findByUserId(userId);
    }

    public Optional<UserDetails> updateUserProfile(Long userId, String address, String phoneNumber, LocalDate birthDate) {
        Optional<UserDetails> detailsOpt = userDetailsRepository.findByUserId(userId);
        if (detailsOpt.isPresent()) {
            UserDetails details = detailsOpt.get();
            if (address != null) details.setAddress(address);
            if (phoneNumber != null) details.setPhoneNumber(phoneNumber);
            if (birthDate != null) details.setBirthDate(birthDate);
            userDetailsRepository.save(details);
            return Optional.of(details);
        }
        return Optional.empty();
    }

    public boolean changePassword(Long userId, String currentPassword, String newPassword) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(currentPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }

    public boolean changeUsername(Long userId, String newUsername) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setUsername(newUsername);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public boolean deleteUser(Long currentUserId, Long targetUserId) {
        // Kullanıcı kendisini silemesin
        if (currentUserId.equals(targetUserId)) {
            return false;
        }
        
        // Hedef kullanıcıyı bul
        Optional<User> targetUserOpt = userRepository.findById(targetUserId);
        if (!targetUserOpt.isPresent()) {
            return false;
        }
        
        // Mevcut kullanıcıyı bul
        Optional<User> currentUserOpt = userRepository.findById(currentUserId);
        if (!currentUserOpt.isPresent()) {
            return false;
        }
        
        User currentUser = currentUserOpt.get();
        User targetUser = targetUserOpt.get();
        
        // Sadece ADMIN başka kullanıcıları silebilir
        if (!currentUser.getRole().equals(Role.ROLE_ADMIN)) {
            return false;
        }
        
        // Admin kendisini silemesin
        if (currentUser.getRole().equals(Role.ROLE_ADMIN) && currentUserId.equals(targetUserId)) {
            return false;
        }
        
        // Kullanıcıyı sil
        userRepository.delete(targetUser);
        
        // Kullanıcı detaylarını da sil
        userDetailsRepository.findByUserId(targetUserId).ifPresent(userDetailsRepository::delete);
        
        return true;
    }
}