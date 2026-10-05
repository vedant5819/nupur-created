package com.example.qrattendance.repository;

import com.example.qrattendance.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // Check if an attendance record exists for a student in a specific session
    boolean existsBySessionIdAndStudentId(Long sessionId, String studentId);

    // Check if an attendance record exists for a studentId between startOfDay and endOfDay (legacy)
    boolean existsByStudentIdAndAttendanceTimeBetween(String studentId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    // Find all attendance records for a specific session ID
    List<Attendance> findBySessionId(Long sessionId);

    // Count attendance records for a specific session ID
    long countBySessionId(Long sessionId);

    // Find all attendance records for a specific student, most recent first
    List<Attendance> findByStudentIdOrderByAttendanceTimeDesc(String studentId);
}
