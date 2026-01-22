package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.model.ActivationToken;
import com.example.demo.model.User;
import com.example.demo.repository.ActivationTokenRepository;
import com.example.demo.repository.UserRepository;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivationTokenRepository tokenRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerNewUser(User user, String appUrl) throws IllegalArgumentException {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already in use");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEnabled(false);
        User saved = userRepository.save(user);

        ActivationToken token = new ActivationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(saved);
        token.setExpiry(LocalDateTime.now().plusDays(1));
        tokenRepository.save(token);

        String link = appUrl + "/api/activate?token=" + token.getToken();
        emailService.sendActivationEmail(saved.getEmail(), link);
        return saved;
    }

    public boolean activateUser(String tokenStr) {
        Optional<ActivationToken> ot = tokenRepository.findByToken(tokenStr);
        if (!ot.isPresent()) return false;
        ActivationToken token = ot.get();
        if (token.getExpiry().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(token);
            return false;
        }
        User u = token.getUser();
        u.setEnabled(true);
        userRepository.save(u);
        tokenRepository.delete(token);
        return true;
    }

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
