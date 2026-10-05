package com.example.qrattendance;

import com.example.qrattendance.model.Attendance;
import com.example.qrattendance.model.AttendanceSession;
import com.example.qrattendance.model.User;
import com.example.qrattendance.repository.AttendanceRepository;
import com.example.qrattendance.repository.AttendanceSessionRepository;
import com.example.qrattendance.repository.UserRepository;
import com.example.qrattendance.service.AttendanceService;
import com.example.qrattendance.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class QrAttendanceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceSessionRepository sessionRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        attendanceRepository.deleteAll();
        sessionRepository.deleteAll();
        // Keep default initialized users or ensure test users exist
    }

    @Test
    @DisplayName("Test 1: Student Registration - saves user and hashes password")
    void testStudentRegistration() {
        String testStudentId = "TEST_CS_2026";
        String testEmail = "test_student@college.edu";

        // Remove if exists
        userRepository.findByStudentId(testStudentId).ifPresent(userRepository::delete);

        userService.registerStudent("Test Student", testStudentId, testEmail, "secretPass", "secretPass");

        Optional<User> savedUserOpt = userRepository.findByStudentId(testStudentId);
        assertTrue(savedUserOpt.isPresent(), "User should exist in database");

        User savedUser = savedUserOpt.get();
        assertEquals("Test Student", savedUser.getName());
        assertEquals("ROLE_STUDENT", savedUser.getRole());
        assertNotEquals("secretPass", savedUser.getPassword(), "Password must not be stored in plaintext");
        assertTrue(passwordEncoder.matches("secretPass", savedUser.getPassword()), "Password must match hashed BCrypt password");
    }

    @Test
    @DisplayName("Test 2 & 3: Login verification via Form Login")
    void testFormLoginValidAndInvalid() throws Exception {
        // Valid teacher login -> redirects to /teacher
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "teacher")
                        .param("password", "teacher123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teacher"));

        // Valid student login -> redirects to /student
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "2024CS101")
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/student"));

        // Invalid login -> redirects to /login?error=true
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "2024CS101")
                        .param("password", "wrong_password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    @DisplayName("Test 4 & 5: Role-based Authorization and Access Control")
    void testRoleAccessControl() throws Exception {
        // Unauthenticated access to /teacher -> redirected to login
        mockMvc.perform(get("/teacher"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Unauthenticated access to /student -> redirected to login
        mockMvc.perform(get("/student"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Student accessing /teacher -> 403 Forbidden
        mockMvc.perform(get("/teacher").with(user("2024CS101").roles("STUDENT")))
                .andExpect(status().isForbidden());

        // Teacher accessing /teacher -> 200 OK
        mockMvc.perform(get("/teacher").with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher"));

        // Student accessing /student -> 200 OK
        mockMvc.perform(get("/student").with(user("2024CS101").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("student"));
    }

    @Test
    @DisplayName("Test 6 & 7: Lecture Session Creation and QR Endpoint")
    void testLectureCreationAndQr() throws Exception {
        // Teacher creates session
        mockMvc.perform(post("/teacher")
                        .with(csrf())
                        .with(user("teacher").roles("TEACHER"))
                        .param("sessionName", "Data Structures - Trees"))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher"))
                .andExpect(model().attributeExists("createdSession"));

        List<AttendanceSession> sessions = sessionRepository.findAll();
        assertFalse(sessions.isEmpty());
        AttendanceSession session = sessions.get(0);
        assertEquals("Data Structures - Trees", session.getSessionName());
        assertTrue(session.isActive());

        // Teacher views QR page for session
        mockMvc.perform(get("/qr/" + session.getId()).with(user("teacher").roles("TEACHER")))
                .andExpect(status().isOk())
                .andExpect(view().name("qr"))
                .andExpect(model().attributeExists("qrCodeImage"))
                .andExpect(model().attribute("attendanceUrl", "http://localhost:8080/attendance/" + session.getId()));
    }

    @Test
    @DisplayName("Test 8, 9, 10, 11: End-to-end Attendance, Duplicate Prevention, Multi-lecture & Inactive Check")
    void testEndToEndAttendanceFlow() throws Exception {
        // 1. Create two active sessions
        AttendanceSession session1 = new AttendanceSession("Physics 101", LocalDateTime.now());
        session1.setActive(true);
        session1 = sessionRepository.save(session1);

        AttendanceSession session2 = new AttendanceSession("Chemistry 101", LocalDateTime.now());
        session2.setActive(true);
        session2 = sessionRepository.save(session2);

        // 2. Student scans session 1 and marks attendance
        mockMvc.perform(post("/attendance/" + session1.getId())
                        .with(csrf())
                        .with(user("2024CS101").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("attendance"))
                .andExpect(model().attribute("isError", false))
                .andExpect(model().attribute("message", "Attendance marked successfully!"));

        // Verify attendance record in database
        List<Attendance> records1 = attendanceRepository.findBySessionId(session1.getId());
        assertEquals(1, records1.size());
        assertEquals("2024CS101", records1.get(0).getStudentId());
        assertEquals("Rahul Sharma", records1.get(0).getStudentName());

        // 3. Duplicate check: Same student tries to mark session 1 again
        mockMvc.perform(post("/attendance/" + session1.getId())
                        .with(csrf())
                        .with(user("2024CS101").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("attendance"))
                .andExpect(model().attribute("isError", true))
                .andExpect(model().attribute("message", "You have already marked attendance for this lecture."));

        // Verify no duplicate record created
        assertEquals(1, attendanceRepository.countBySessionId(session1.getId()));

        // 4. Same student marks attendance for different lecture (session 2) -> Should succeed!
        mockMvc.perform(post("/attendance/" + session2.getId())
                        .with(csrf())
                        .with(user("2024CS101").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("attendance"))
                .andExpect(model().attribute("isError", false))
                .andExpect(model().attribute("message", "Attendance marked successfully!"));

        assertEquals(1, attendanceRepository.countBySessionId(session2.getId()));

        // 5. Teacher closes session 2
        session2.setActive(false);
        sessionRepository.save(session2);

        // Another student attempts to mark attendance for closed session 2
        // Register a second student
        if (!userRepository.existsByStudentId("2024CS102")) {
            userService.registerStudent("Priya Patel", "2024CS102", "priya@college.edu", "pass123", "pass123");
        }

        mockMvc.perform(post("/attendance/" + session2.getId())
                        .with(csrf())
                        .with(user("2024CS102").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("attendance"))
                .andExpect(model().attribute("sessionError", "This attendance session has ended."));
    }

    @Test
    @DisplayName("Test 12: Unauthenticated student scanning QR is redirected to login")
    void testUnauthenticatedStudentRedirect() throws Exception {
        AttendanceSession session = new AttendanceSession("Biology 101", LocalDateTime.now());
        session.setActive(true);
        session = sessionRepository.save(session);

        mockMvc.perform(get("/attendance/" + session.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
