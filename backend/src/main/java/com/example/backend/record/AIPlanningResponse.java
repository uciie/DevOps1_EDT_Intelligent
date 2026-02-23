package com.example.backend.record;

import java.util.List;

public record AIPlanningResponse(
    String globalExplanation,
    List<AIProposedTask> tasks
) {}