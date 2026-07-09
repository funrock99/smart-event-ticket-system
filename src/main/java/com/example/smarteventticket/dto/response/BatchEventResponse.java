package com.example.smarteventticket.dto.response;

import java.util.List;

public record BatchEventResponse(
        int total,
        int created,
        int duplicated,
        int rateLimited,
        List<EventIngestionResponse> results
) {
}
