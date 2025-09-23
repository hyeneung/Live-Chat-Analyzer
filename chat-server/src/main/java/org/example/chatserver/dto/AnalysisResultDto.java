package org.example.chatserver.dto;

import java.util.Map;

public record AnalysisResultDto(
    String streamId,
    int totalCount,
    Map<String, Double> ratios
) {
}
