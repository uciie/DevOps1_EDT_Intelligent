package com.example.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.model.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // Récupère les 10 derniers messages, triés du plus récent au plus ancien
    List<ChatMessage> findTop10ByUserIdOrderByTimestampDesc(Long userId);
}