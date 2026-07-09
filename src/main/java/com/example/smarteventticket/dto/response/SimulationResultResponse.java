package com.example.smarteventticket.dto.response;

public record SimulationResultResponse(
        String source,
        int requested,
        int created,
        int duplicated,
        int rateLimited
) {
}
