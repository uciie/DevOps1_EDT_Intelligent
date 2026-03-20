package com.example.backend.record;


import java.util.List;

public record AIHistoryRequest(
    String userFeedback, 
    List<AIProposedTask> currentTasks
) {}