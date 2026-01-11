package com.example.demo.controller.api;

import com.example.demo.model.User;
import com.example.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class AuthApiController {

    private final UserService userService;

    public AuthApiController(UserService userService) {
        this.userService = userService;
    }

    public record RegisterRequest(
            String email,
            String username,
            String password,
            String passwordConfirm
    ) {}

    public record MessageResponse(String message) {}

    @PostMapping("/register")
    public MessageResponse register(@RequestBody RegisterRequest req, HttpServletRequest request) {
        if (req.password() == null || !req.password().equals(req.passwordConfirm())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }

        // Napravi User objekat iz request-a
        User user = new User();
        user.setEmail(req.email());
        user.setUsername(req.username());
        user.setPassword(req.password()); // UserService će hashovati (nadam se) ili radiš ovde

        String appUrl = request.getScheme() + "://" + request.getServerName()
                + ((request.getServerPort() == 80 || request.getServerPort() == 443) ? "" : ":" + request.getServerPort());

        try {
            userService.registerNewUser(user, appUrl);
            return new MessageResponse("Registration successful. Check your email to activate your account.");
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/activate")
    public MessageResponse activate(@RequestParam("token") String token) {
        boolean ok = userService.activateUser(token);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Activation failed or token expired.");
        }
        return new MessageResponse("Account activated. You may login now.");
    }
}
