package com.example.smarteventticket.dto.response;

public record DedupStatsResponse(
        long totalProcessedEvents,
        long validEvents,
        long duplicatedEvents,
        double dedupRate
) {
}
