package com.example.smarteventticket.dto.response;

import java.time.LocalDateTime;

public record SourceRankingResponse(
        String source,
        long count,
        String latestEventType,
        String highestSeverity,
        LocalDateTime latestOccurredAt
) {
}
