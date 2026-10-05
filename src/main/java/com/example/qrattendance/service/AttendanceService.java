package com.example.qrattendance.service;

import com.example.qrattendance.model.Attendance;
import com.example.qrattendance.model.AttendanceSession;
import com.example.qrattendance.model.User;
import com.example.qrattendance.repository.AttendanceRepository;
import com.example.qrattendance.repository.AttendanceSessionRepository;
import com.example.qrattendance.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceSessionRepository attendanceSessionRepository;
    private final UserRepository userRepository;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             AttendanceSessionRepository attendanceSessionRepository,
                             UserRepository userRepository) {
        this.attendanceRepository = attendanceRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
        this.userRepository = userRepository;
    }

    public static class AttendanceResult {
        private final boolean success;
        private final String message;
        private final AttendanceSession session;
        private final Attendance attendance;

        public AttendanceResult(boolean success, String message, AttendanceSession session, Attendance attendance) {
            this.success = success;
            this.message = message;
            this.session = session;
            this.attendance = attendance;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public AttendanceSession getSession() {
            return session;
        }

        public Attendance getAttendance() {
            return attendance;
        }
    }

    @Transactional
    public AttendanceResult markAttendance(Long sessionId, String usernameOrStudentId) {
        // 1. Identify authenticated user
        Optional<User> userOpt = userRepository.findByStudentIdOrEmail(usernameOrStudentId, usernameOrStudentId);
        if (userOpt.isEmpty()) {
            return new AttendanceResult(false, "Authenticated student profile not found.", null, null);
        }
        User student = userOpt.get();

        // 2. Load attendance session
        Optional<AttendanceSession> sessionOpt = attendanceSessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return new AttendanceResult(false, "Attendance session not found.", null, null);
        }

        AttendanceSession session = sessionOpt.get();

        // 3. Verify session is active
        if (!session.isActive()) {
            return new AttendanceResult(false, "This attendance session has ended.", session, null);
        }

        // 4. Check for duplicate attendance for this student and this session
        boolean alreadyMarked = attendanceRepository.existsBySessionIdAndStudentId(sessionId, student.getStudentId());
        if (alreadyMarked) {
            return new AttendanceResult(false, "You have already marked attendance for this lecture.", session, null);
        }

        // 5. Create and save attendance record
        Attendance attendance = new Attendance();
        attendance.setStudentId(student.getStudentId());
        attendance.setStudentName(student.getName());
        attendance.setAttendanceTime(LocalDateTime.now());
        attendance.setSession(session);
        attendance.setUser(student);

        Attendance savedAttendance = attendanceRepository.save(attendance);

        return new AttendanceResult(true, "Attendance marked successfully!", session, savedAttendance);
    }
}
