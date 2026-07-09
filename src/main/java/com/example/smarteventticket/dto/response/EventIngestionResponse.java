package com.example.smarteventticket.dto.response;

public record EventIngestionResponse(
        boolean success,
        Long eventId,
        Long ticketId,
        boolean duplicated,
        boolean rateLimited,
        String message
) {
}
