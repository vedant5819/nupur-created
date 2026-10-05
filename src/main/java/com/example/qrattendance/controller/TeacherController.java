package com.example.qrattendance.controller;

import com.example.qrattendance.model.Attendance;
import com.example.qrattendance.model.AttendanceSession;
import com.example.qrattendance.repository.AttendanceRepository;
import com.example.qrattendance.repository.AttendanceSessionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class TeacherController {

    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final com.example.qrattendance.service.UserService userService;

    public TeacherController(AttendanceSessionRepository attendanceSessionRepository,
                             AttendanceRepository attendanceRepository,
                             com.example.qrattendance.service.UserService userService) {
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.userService = userService;
    }

    private void populateDashboardModel(Model model, java.security.Principal principal) {
        List<AttendanceSession> sessions = attendanceSessionRepository.findAll();
        Map<Long, Long> attendanceCounts = new HashMap<>();
        for (AttendanceSession session : sessions) {
            long count = attendanceRepository.countBySessionId(session.getId());
            attendanceCounts.put(session.getId(), count);
        }
        model.addAttribute("sessions", sessions);
        model.addAttribute("attendanceCounts", attendanceCounts);

        if (principal != null) {
            userService.findByStudentIdOrEmail(principal.getName())
                    .ifPresent(teacher -> model.addAttribute("teacher", teacher));
        }
    }

    @GetMapping("/teacher")
    public String showTeacherDashboard(java.security.Principal principal, Model model) {
        populateDashboardModel(model, principal);
        return "teacher";
    }

    @PostMapping("/teacher")
    public String createSession(@RequestParam String sessionName, java.security.Principal principal, Model model) {
        // Create new attendance session
        AttendanceSession attendanceSession = new AttendanceSession();
        attendanceSession.setSessionName(sessionName);
        attendanceSession.setStartTime(LocalDateTime.now());
        attendanceSession.setActive(true);

        // Save session into SQLite database
        AttendanceSession savedSession = attendanceSessionRepository.save(attendanceSession);

        // Pass success message and created session details to the view
        model.addAttribute("message", "Attendance session created successfully!");
        model.addAttribute("createdSession", savedSession);
        populateDashboardModel(model, principal);

        return "teacher";
    }

    @PostMapping("/teacher/session/{sessionId}/close")
    public String closeSession(@PathVariable Long sessionId, java.security.Principal principal, Model model) {
        Optional<AttendanceSession> sessionOpt = attendanceSessionRepository.findById(sessionId);

        if (sessionOpt.isPresent()) {
            AttendanceSession session = sessionOpt.get();
            session.setActive(false);
            session.setEndTime(LocalDateTime.now());
            attendanceSessionRepository.save(session);

            model.addAttribute("message", "Attendance session closed successfully!");
        } else {
            model.addAttribute("error", "Attendance session not found.");
        }

        populateDashboardModel(model, principal);
        return "teacher";
    }

    @GetMapping("/teacher/session/{sessionId}/attendance")
    public String showSessionAttendance(@PathVariable Long sessionId, Model model) {
        Optional<AttendanceSession> sessionOpt = attendanceSessionRepository.findById(sessionId);

        if (sessionOpt.isPresent()) {
            AttendanceSession session = sessionOpt.get();
            List<Attendance> records = attendanceRepository.findBySessionId(sessionId);

            model.addAttribute("attendanceSession", session);
            model.addAttribute("attendanceRecords", records);
        } else {
            model.addAttribute("error", "Attendance session not found.");
        }

        return "session-attendance";
    }
}
