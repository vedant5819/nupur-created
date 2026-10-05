package com.example.qrattendance.controller;

import com.example.qrattendance.model.Attendance;
import com.example.qrattendance.model.User;
import com.example.qrattendance.repository.AttendanceRepository;
import com.example.qrattendance.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
public class StudentController {

    private final UserService userService;
    private final AttendanceRepository attendanceRepository;

    public StudentController(UserService userService, AttendanceRepository attendanceRepository) {
        this.userService = userService;
        this.attendanceRepository = attendanceRepository;
    }

    @GetMapping("/student")
    public String showStudentDashboard(Principal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }

        Optional<User> userOpt = userService.findByStudentIdOrEmail(principal.getName());
        if (userOpt.isPresent()) {
            User student = userOpt.get();
            model.addAttribute("student", student);

            List<Attendance> attendances = attendanceRepository.findByStudentIdOrderByAttendanceTimeDesc(student.getStudentId());
            model.addAttribute("attendances", attendances);
        }

        return "student";
    }
}
