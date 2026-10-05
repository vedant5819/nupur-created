# QR Code Attendance System 📱🎓

A lightweight, session-based **QR Code Attendance System** built with **Java 21**, **Spring Boot 3.3.4**, **Thymeleaf**, and **SQLite**. This application allows teachers to create attendance sessions, generate dynamic QR codes for students to scan, prevent duplicate submissions, and view live student attendance counts in real-time.

---

## 🚀 Features

- **Teacher Dashboard**:
  - Create unique attendance sessions with custom session names.
  - View real-time **Students Present** count for each session.
  - Close active sessions when attendance window ends.
  - View detailed student attendance logs per session.
- **Dynamic QR Code Generation**:
  - Automatically generates scannable QR codes containing session-specific URL parameters using the ZXing library.
- **Session-Based Attendance Submission**:
  - Direct form for students to submit ID and Name.
  - Automatically verifies session activity (blocks attendance if closed or invalid).
- **Duplicate Attendance Prevention**:
  - Prevents students from submitting attendance multiple times on the same day for a session.
- **Zero Heavy Setup**:
  - Uses embedded SQLite database — no external database server installation required.

---

## 🛠️ Technology Stack & Dependencies

| Technology | Purpose | Version |
| :--- | :--- | :--- |
| **Java** | Core Programming Language | `21` |
| **Spring Boot** | Framework (Web MVC & JPA) | `3.3.4` |
| **Thymeleaf** | Server-side HTML Template Engine | Standard |
| **SQLite JDBC** | Embedded Database Driver | `org.xerial:sqlite-jdbc` |
| **Hibernate Dialects**| Community SQLite Dialect | `org.hibernate.orm:hibernate-community-dialects` |
| **ZXing (Zebra Crossing)**| QR Code Matrix & Image Generation | `3.5.3` |
| **Maven Wrapper** | Build Tool | Included (`mvnw` / `mvnw.cmd`) |

---

## 📁 Project Structure

```text
QR-attendance-system/
├── src/
│   ├── main/
│   │   ├── java/com/example/qrattendance/
│   │   │   ├── QrAttendanceApplication.java      # Main Application Entry Point
│   │   │   ├── controller/
│   │   │   │   ├── HomeController.java          # Home Page Routing
│   │   │   │   ├── TeacherController.java       # Session Management & Dashboard
│   │   │   │   ├── AttendanceController.java    # Student Attendance Logic
│   │   │   │   └── QRCodeController.java        # QR Image Generation Logic
│   │   │   ├── model/
│   │   │   │   ├── AttendanceSession.java       # Session Entity
│   │   │   │   └── Attendance.java              # Student Attendance Record Entity
│   │   │   └── repository/
│   │   │       ├── AttendanceSessionRepository.java
│   │   │       └── AttendanceRepository.java
│   │   └── resources/
│   │       ├── templates/                       # Thymeleaf Views
│   │       │   ├── index.html                   # Home Page
│   │       │   ├── teacher.html                 # Teacher Dashboard
│   │       │   ├── qr.html                      # QR Code Display Page
│   │       │   ├── attendance.html              # Student Attendance Form
│   │       │   └── session-attendance.html      # Detailed Attendance Records
│   │       └── application.properties           # Application Configuration
├── pom.xml                                      # Maven Dependencies & Build Setup
├── mvnw / mvnw.cmd                              # Maven Wrappers
└── README.md                                    # Project Documentation
```

---

## ⚙️ Configuration

Open `src/main/resources/application.properties`:

```properties
# Server Configuration
spring.application.name=qr-attendance
server.port=8080

# SQLite Database
spring.datasource.url=jdbc:sqlite:attendance.db
spring.datasource.driver-class-name=org.sqlite.JDBC

# JPA / Hibernate
spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Attendance QR Code Base URL (Set to your local Wi-Fi IP address for mobile scanning)
app.attendance-url=http://192.168.1.230:8080/attendance
```

> 💡 **Tip for Mobile Scanning**: Replace `192.168.1.230` with your machine's local IP address (find via `ipconfig` on Windows or `ifconfig` on Mac/Linux) so mobile phones connected to the same Wi-Fi network can scan the QR code and submit attendance.

---

## 💻 How to Run Locally

### Prerequisites
- **JDK 21** installed and configured in your environment PATH (`java -version`).
- **Git** (optional, to clone repository).

### Steps:

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/<your-username>/QR-attendance-system.git
   cd QR-attendance-system
   ```

2. **Verify Dependencies & Build**:
   - **Windows**:
     ```cmd
     .\mvnw.cmd clean compile
     ```
   - **Linux / macOS**:
     ```bash
     chmod +x mvnw
     ./mvnw clean compile
     ```

3. **Start the Application**:
   - **Windows**:
     ```cmd
     .\mvnw.cmd spring-boot:run
     ```
   - **Linux / macOS**:
     ```bash
     ./mvnw spring-boot:run
     ```

4. **Access the Application**:
   Open your browser and navigate to:
   - **Home Page**: [http://localhost:8080](http://localhost:8080)
   - **Teacher Dashboard**: [http://localhost:8080/teacher](http://localhost:8080/teacher)

---

## 📌 Routes & API Overview

| Route | Method | Description |
| :--- | :--- | :--- |
| `/` | `GET` | Home page landing navigation |
| `/teacher` | `GET` | Teacher Dashboard displaying sessions & live student counts |
| `/teacher` | `POST` | Create a new attendance session |
| `/teacher/session/{id}/close` | `POST` | Close an active attendance session |
| `/teacher/session/{id}/attendance` | `GET` | View student attendance table for a specific session |
| `/qr/{sessionId}` | `GET` | View generated QR Code for a specific session |
| `/attendance/{sessionId}` | `GET` | Student form page to mark attendance for a session |
| `/attendance/{sessionId}` | `POST` | Submit student ID & name for attendance marking |

---

## 📋 Pre-Push GitHub Checklist

Before pushing your repository to GitHub, ensure:

- [x] All Maven dependencies compile cleanly (`.\mvnw.cmd clean compile` / `./mvnw clean compile`).
- [x] `.gitignore` file is included in root directory so build targets (`target/`) and local database files (`attendance.db`) are excluded.
- [x] Application properties configured properly.
- [x] Local code tested and working as expected.

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
