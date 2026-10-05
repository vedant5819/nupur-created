package com.example.qrattendance.controller;

import com.example.qrattendance.model.User;
import com.example.qrattendance.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.Optional;

@Controller
public class HomeController {

    private final UserService userService;

    public HomeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(Principal principal, Model model) {
        if (principal != null) {
            Optional<User> userOpt = userService.findByStudentIdOrEmail(principal.getName());
            userOpt.ifPresent(user -> model.addAttribute("currentUser", user));
        }
        return "index";
    }
}
