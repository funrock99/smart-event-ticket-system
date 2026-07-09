package com.example.smarteventticket.dto.response;

public record DashboardSummaryResponse(
        long totalEvents,
        long validEvents,
        long duplicatedEvents,
        long rateLimitedEvents,
        long idempotentReplayedEvents,
        long openTickets,
        long processingTickets,
        long resolvedTickets,
        long closedTickets
) {
}
