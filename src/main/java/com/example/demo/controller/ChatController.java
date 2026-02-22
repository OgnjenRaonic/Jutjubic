package com.example.demo.controller;

import java.security.Principal;
import java.time.Instant;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.example.demo.dtos.ChatMessage;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/streams/{videoId}/chat")
    public void send(@DestinationVariable Long videoId, ChatMessage incoming, Principal principal) {
        String sender = resolveSender(incoming, principal);
        String content = incoming != null ? incoming.getContent() : null;

        if (content == null || content.trim().isEmpty()) {
            return;
        }

        ChatMessage outgoing = new ChatMessage(
                videoId,
                sender,
                content.trim(),
                Instant.now().toString()
        );

        messagingTemplate.convertAndSend("/topic/streams/" + videoId, outgoing);
    }

    private String resolveSender(ChatMessage incoming, Principal principal) {
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return principal.getName();
        }
        if (incoming != null && incoming.getSender() != null && !incoming.getSender().isBlank()) {
            return incoming.getSender().trim();
        }
        return "anon";
    }
}
