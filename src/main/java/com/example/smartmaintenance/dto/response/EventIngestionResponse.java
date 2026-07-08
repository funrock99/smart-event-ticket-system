package com.example.smartmaintenance.dto.response;

public record EventIngestionResponse(
        boolean success,
        Long eventId,
        Long ticketId,
        boolean duplicated,
        boolean rateLimited,
        String message
) {
}