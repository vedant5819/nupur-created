package com.example.qrattendance.controller;

import com.example.qrattendance.model.AttendanceSession;
import com.example.qrattendance.model.User;
import com.example.qrattendance.repository.AttendanceRepository;
import com.example.qrattendance.repository.AttendanceSessionRepository;
import com.example.qrattendance.service.AttendanceService;
import com.example.qrattendance.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.security.Principal;
import java.util.Optional;

@Controller
public class AttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final AttendanceService attendanceService;
    private final UserService userService;

    public AttendanceController(AttendanceRepository attendanceRepository,
                                AttendanceSessionRepository attendanceSessionRepository,
                                AttendanceService attendanceService,
                                UserService userService) {
        this.attendanceRepository = attendanceRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.attendanceService = attendanceService;
        this.userService = userService;
    }

    // Default route when no sessionId is given
    @GetMapping("/attendance")
    public String showAttendanceDefault(Principal principal, Model model) {
        if (principal != null) {
            userService.findByStudentIdOrEmail(principal.getName())
                    .ifPresent(user -> model.addAttribute("student", user));
        }
        model.addAttribute("sessionError", "Please scan a valid lecture QR code to mark attendance.");
        return "attendance";
    }

    // Session-specific GET route: /attendance/{sessionId}
    @GetMapping("/attendance/{sessionId}")
    public String showSessionAttendancePage(@PathVariable Long sessionId,
                                            Principal principal,
                                            Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        Optional<User> userOpt = userService.findByStudentIdOrEmail(principal.getName());
        if (userOpt.isEmpty()) {
            model.addAttribute("sessionError", "Student profile not found. Please re-login.");
            return "attendance";
        }
        User student = userOpt.get();
        model.addAttribute("student", student);

        Optional<AttendanceSession> sessionOpt = attendanceSessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            model.addAttribute("sessionError", "Attendance session not found.");
            return "attendance";
        }

        AttendanceSession session = sessionOpt.get();
        model.addAttribute("attendanceSession", session);

        if (!session.isActive()) {
            model.addAttribute("sessionError", "This attendance session has ended.");
            return "attendance";
        }

        // Check if already attended
        boolean alreadyMarked = attendanceRepository.existsBySessionIdAndStudentId(sessionId, student.getStudentId());
        if (alreadyMarked) {
            model.addAttribute("message", "You have already marked attendance for this lecture.");
            model.addAttribute("isError", true);
            model.addAttribute("alreadyMarked", true);
        }

        return "attendance";
    }

    // Session-specific POST route: /attendance/{sessionId}
    @PostMapping("/attendance/{sessionId}")
    public String markSessionAttendance(@PathVariable Long sessionId,
                                        Principal principal,
                                        Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        Optional<User> userOpt = userService.findByStudentIdOrEmail(principal.getName());
        if (userOpt.isEmpty()) {
            model.addAttribute("sessionError", "Student profile not found. Please re-login.");
            return "attendance";
        }
        User student = userOpt.get();
        model.addAttribute("student", student);

        AttendanceService.AttendanceResult result = attendanceService.markAttendance(sessionId, principal.getName());

        if (result.getSession() != null) {
            model.addAttribute("attendanceSession", result.getSession());
        }

        if (result.isSuccess()) {
            model.addAttribute("message", result.getMessage());
            model.addAttribute("isError", false);
            model.addAttribute("markedSuccess", true);
        } else {
            // Check if error is because session ended or duplicate or not found
            if (result.getSession() != null && !result.getSession().isActive()) {
                model.addAttribute("sessionError", result.getMessage());
            } else {
                model.addAttribute("message", result.getMessage());
                model.addAttribute("isError", true);
            }
        }

        return "attendance";
    }
}
