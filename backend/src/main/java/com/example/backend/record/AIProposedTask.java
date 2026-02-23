package com.example.backend.record;

import java.util.List;

public record AIProposedTask(
    String id, 
    String title, 
    int durationMinutes, 
    List<String> dependencies, 
    String reasoning // Ce que l'IA explique via le Chain of Thought
) {}

