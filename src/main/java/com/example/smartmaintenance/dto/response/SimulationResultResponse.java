package com.example.smartmaintenance.dto.response;

public record SimulationResultResponse(
        String source,
        int requested,
        int created,
        int duplicated,
        int rateLimited
) {
}