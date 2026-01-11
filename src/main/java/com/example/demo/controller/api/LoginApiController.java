package com.example.demo.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class LoginApiController {

    private final AuthenticationManager authenticationManager;

    public LoginApiController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    public record LoginRequest(String email, String password) {}
    public record MessageResponse(String message) {}

    @PostMapping("/login")
    public MessageResponse login(@RequestBody LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email(), req.password())
            );
            return new MessageResponse("Login OK");
        } catch (AuthenticationException e) {
            // Ovo će ti reći: BadCredentialsException, DisabledException, LockedException...
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    e.getClass().getSimpleName() + ": " + e.getMessage()
            );
        }
    }
}
