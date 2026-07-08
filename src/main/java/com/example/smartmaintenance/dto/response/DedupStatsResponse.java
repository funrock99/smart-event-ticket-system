package com.example.smartmaintenance.dto.response;

public record DedupStatsResponse(
        long totalProcessedEvents,
        long validEvents,
        long duplicatedEvents,
        double dedupRate
) {
}