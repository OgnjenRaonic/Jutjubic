package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dtos.LoginRequestDTO;
import com.example.demo.model.User;
import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public AuthController(AuthenticationManager authenticationManager, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO dto,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            request.getSession(true);
            securityContextRepository.saveContext(context, request, response);

            return ResponseEntity.ok(new LoginResponse(auth.getName(), "Login successful"));
        } catch (DisabledException e) {
            return ResponseEntity.status(403).body(new LoginResponse(null, "Account not activated. Check your email for activation link."));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(new LoginResponse(null, "Invalid email or password"));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(new LoginResponse(null, "Authentication failed: " + e.getMessage()));
        }
    }
    @GetMapping("/activate")
    public ResponseEntity<?> activate(@RequestParam String token) {
        boolean activated = userService.activateUser(token);
        if (activated) {
            return ResponseEntity.ok("Account activated successfully");
        } else {
            return ResponseEntity.badRequest().body("Invalid or expired token");
        }
    }

    

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(new MeResponse(auth.getName()));
    }

    // Debug endpoint - pregledi status korisnika
    @GetMapping("/user-status/{email}")
    public ResponseEntity<?> userStatus(@PathVariable String email) {
        try {
            User u = userService.findUserByEmail(email);
            return ResponseEntity.ok(new UserStatusResponse(
                u.getEmail(),
                u.getUsername(),
                u.isEnabled(),
                "User found in database"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(new UserStatusResponse(null, null, false, "User not found: " + e.getMessage()));
        }
    }

    public record MeResponse(String email) {}
    public record LoginResponse(String email, String message) {}
    public record UserStatusResponse(String email, String username, boolean enabled, String message) {}
}
