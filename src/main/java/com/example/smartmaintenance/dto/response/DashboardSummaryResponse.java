package com.example.smartmaintenance.dto.response;

public record DashboardSummaryResponse(
        long totalEvents,
        long validEvents,
        long duplicatedEvents,
        long rateLimitedEvents,
        long openTickets,
        long processingTickets,
        long resolvedTickets,
        long closedTickets
) {
}