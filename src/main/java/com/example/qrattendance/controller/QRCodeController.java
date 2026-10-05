package com.example.qrattendance.controller;

import com.example.qrattendance.model.AttendanceSession;
import com.example.qrattendance.repository.AttendanceSessionRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Optional;

@Controller
public class QRCodeController {

    @Value("${app.attendance-url}")
    private String attendanceUrl;

    private final AttendanceSessionRepository attendanceSessionRepository;

    public QRCodeController(AttendanceSessionRepository attendanceSessionRepository) {
        this.attendanceSessionRepository = attendanceSessionRepository;
    }

    // Legacy QR page endpoint
    @GetMapping("/qr")
    public String showQrCodePage(Model model) {
        generateQrCode(attendanceUrl, model);
        return "qr";
    }

    // Session-specific QR page endpoint: /qr/{sessionId}
    @GetMapping("/qr/{sessionId}")
    public String showSessionQrCodePage(@PathVariable Long sessionId, Model model) {
        Optional<AttendanceSession> sessionOpt = attendanceSessionRepository.findById(sessionId);

        if (sessionOpt.isEmpty()) {
            model.addAttribute("error", "Attendance session not found.");
            return "qr";
        }

        AttendanceSession session = sessionOpt.get();
        if (!session.isActive()) {
            model.addAttribute("error", "This attendance session is closed.");
            model.addAttribute("attendanceSession", session);
            return "qr";
        }

        String targetUrl = attendanceUrl + "/" + sessionId;
        model.addAttribute("attendanceSession", session);
        generateQrCode(targetUrl, model);

        return "qr";
    }

    private void generateQrCode(String targetUrl, Model model) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(targetUrl, BarcodeFormat.QR_CODE, 250, 250);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            String base64Image = Base64.getEncoder().encodeToString(outputStream.toByteArray());

            model.addAttribute("qrCodeImage", "data:image/png;base64," + base64Image);
            model.addAttribute("attendanceUrl", targetUrl);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to generate QR code.");
        }
    }
}
