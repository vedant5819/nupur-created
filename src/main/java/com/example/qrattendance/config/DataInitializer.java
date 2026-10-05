package com.example.qrattendance.config;

import com.example.qrattendance.model.User;
import com.example.qrattendance.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Seed default Teacher account if not present
        if (!userRepository.existsByStudentId("teacher")) {
            User teacher = new User();
            teacher.setStudentId("teacher");
            teacher.setName("Prof. Sharma");
            teacher.setEmail("teacher@college.edu");
            teacher.setPassword(passwordEncoder.encode("teacher123"));
            teacher.setRole("ROLE_TEACHER");
            userRepository.save(teacher);
            System.out.println(">>> Initialized default Teacher account: username='teacher', password='teacher123'");
        }

        // Seed demo Student account if not present
        if (!userRepository.existsByStudentId("2024CS101")) {
            User student = new User();
            student.setStudentId("2024CS101");
            student.setName("Rahul Sharma");
            student.setEmail("rahul@college.edu");
            student.setPassword(passwordEncoder.encode("password123"));
            student.setRole("ROLE_STUDENT");
            userRepository.save(student);
            System.out.println(">>> Initialized demo Student account: studentId='2024CS101', password='password123'");
        }
    }
}
