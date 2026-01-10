package com.example.demo.controller;

import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {
    @Autowired
    private UserService userService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) model.addAttribute("error", "Invalid credentials or account not activated.");
        return "login";
    }

    @GetMapping("/activate")
    public String activate(@RequestParam("token") String token, Model model) {
        boolean ok = userService.activateUser(token);
        if (ok) model.addAttribute("message", "Account activated. You may login now.");
        else model.addAttribute("error", "Activation failed or token expired.");
        return "login";
    }
}
