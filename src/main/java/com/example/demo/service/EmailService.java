package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired(required = false)
    private JavaMailSender mailSender;

    public void sendActivationEmail(String to, String link) {
        String subject = "Activate your account";
        String body = "Please activate your account by visiting: " + link;
        if (mailSender != null) {
            SimpleMailMessage m = new SimpleMailMessage();
            m.setTo(to);
            m.setSubject(subject);
            m.setText(body);
            mailSender.send(m);
        } else {
            System.out.println("[EMAIL MOCK] To: " + to + " Subject: " + subject + "\n" + body);
        }
    }
}
