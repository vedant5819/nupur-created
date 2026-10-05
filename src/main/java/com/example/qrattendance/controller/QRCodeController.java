package com.example.qrattendance.controller;

import com.example.qrattendance.model.AttendanceSession;
import com.example.qrattendance.repository.AttendanceSessionRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Base64;
import java.util.Enumeration;
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
    public String showQrCodePage(HttpServletRequest request, Model model) {
        String baseUrl = resolveBaseUrl(request, null);
        generateQrCode(baseUrl, model);
        return "qr";
    }

    // Session-specific QR page endpoint: /qr/{sessionId}
    @GetMapping("/qr/{sessionId}")
    public String showSessionQrCodePage(@PathVariable Long sessionId,
                                        @RequestParam(value = "customBaseUrl", required = false) String customBaseUrl,
                                        HttpServletRequest request,
                                        Model model) {
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

        String baseUrl = resolveBaseUrl(request, customBaseUrl);
        String targetUrl = baseUrl + "/" + sessionId;

        model.addAttribute("attendanceSession", session);
        model.addAttribute("baseUrl", baseUrl);
        model.addAttribute("detectedLanIp", getLocalLanIp());
        model.addAttribute("serverPort", request.getServerPort());

        generateQrCode(targetUrl, model);

        return "qr";
    }

    private String resolveBaseUrl(HttpServletRequest request, String customBaseUrl) {
        if (customBaseUrl != null && !customBaseUrl.trim().isEmpty()) {
            String trimmed = customBaseUrl.trim();
            if (trimmed.endsWith("/")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            return trimmed;
        }

        // If explicitly set to something other than localhost
        if (attendanceUrl != null && !attendanceUrl.contains("localhost") && !attendanceUrl.contains("127.0.0.1")) {
            return attendanceUrl;
        }

        // Check reverse proxy / cloud headers (Render, Heroku, Railway, etc.)
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedHost != null && !forwardedHost.isEmpty()) {
            String proto = (forwardedProto != null && !forwardedProto.isEmpty()) ? forwardedProto : request.getScheme();
            return proto + "://" + forwardedHost + "/attendance";
        }

        String serverName = request.getServerName();
        int serverPort = request.getServerPort();

        // If accessed directly via an external IP or cloud domain
        if (!"localhost".equalsIgnoreCase(serverName) && !"127.0.0.1".equals(serverName)) {
            String portStr = (serverPort == 80 || serverPort == 443) ? "" : (":" + serverPort);
            return request.getScheme() + "://" + serverName + portStr + "/attendance";
        }

        // If accessed via localhost, auto-detect the local Wi-Fi / LAN IP address so phones can scan!
        String lanIp = getLocalLanIp();
        if (lanIp != null) {
            String portStr = (serverPort == 80 || serverPort == 443) ? "" : (":" + serverPort);
            return "http://" + lanIp + portStr + "/attendance";
        }

        return attendanceUrl;
    }

    private String getLocalLanIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void generateQrCode(String targetUrl, Model model) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(targetUrl, BarcodeFormat.QR_CODE, 280, 280);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            String base64Image = Base64.getEncoder().encodeToString(outputStream.toByteArray());

            model.addAttribute("qrCodeImage", "data:image/png;base64," + base64Image);
            model.addAttribute("attendanceUrl", targetUrl);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to generate QR code: " + e.getMessage());
        }
    }
}
