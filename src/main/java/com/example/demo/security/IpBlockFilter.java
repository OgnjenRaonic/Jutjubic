package com.example.demo.security;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class IpBlockFilter extends OncePerRequestFilter {
    private final LoginAttemptService loginAttemptService;

    public IpBlockFilter(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod()) && "/login".equals(request.getRequestURI())) {
            String ip = request.getRemoteAddr();
            if (loginAttemptService.isBlocked(ip)) {
                response.setStatus(429);
                response.getWriter().write("Too many login attempts. Try again later.");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
