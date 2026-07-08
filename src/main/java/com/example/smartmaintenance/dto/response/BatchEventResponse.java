package com.example.smartmaintenance.dto.response;

import java.util.List;

public record BatchEventResponse(
        int total,
        int created,
        int duplicated,
        int rateLimited,
        List<EventIngestionResponse> results
) {
}