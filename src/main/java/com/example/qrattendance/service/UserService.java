package com.example.qrattendance.service;

import com.example.qrattendance.model.User;
import com.example.qrattendance.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> findByStudentId(String studentId) {
        return userRepository.findByStudentId(studentId);
    }

    public Optional<User> findByStudentIdOrEmail(String identifier) {
        return userRepository.findByStudentIdOrEmail(identifier, identifier);
    }

    public void registerStudent(String name, String studentId, String email, String password, String confirmPassword) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Full Name is required.");
        }
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID is required.");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address is required.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Password and Confirm Password do not match.");
        }

        String cleanStudentId = studentId.trim();
        String cleanEmail = email.trim().toLowerCase();

        if (userRepository.existsByStudentId(cleanStudentId)) {
            throw new IllegalArgumentException("Student ID '" + cleanStudentId + "' is already registered.");
        }
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("Email '" + cleanEmail + "' is already registered.");
        }

        User user = new User();
        user.setName(name.trim());
        user.setStudentId(cleanStudentId);
        user.setEmail(cleanEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("ROLE_STUDENT");

        userRepository.save(user);
    }
}
