package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.demo.model.User;
import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class RegistrationController {
    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(User user, String passwordConfirm, Model model, HttpServletRequest request) {
        if (user.getPassword() == null || !user.getPassword().equals(passwordConfirm)) {
            model.addAttribute("error", "Passwords do not match");
            model.addAttribute("user", user);
            return "register";
        }
        try {
            String appUrl = request.getScheme() + "://" + request.getServerName() + (request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":"+request.getServerPort());
            userService.registerNewUser(user, appUrl);
            model.addAttribute("message", "Registration successful. Check your email to activate your account.");
            return "login";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", user);
            return "register";
        }
    }
}
