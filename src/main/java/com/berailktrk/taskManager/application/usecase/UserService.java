package com.berailktrk.taskManager.application.usecase;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.berailktrk.taskManager.domain.model.Role;
import com.berailktrk.taskManager.domain.model.User;
import com.berailktrk.taskManager.domain.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    
}