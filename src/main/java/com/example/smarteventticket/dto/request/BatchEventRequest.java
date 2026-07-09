package com.example.smarteventticket.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BatchEventRequest(
        @NotEmpty List<@Valid CreateEventRequest> events
) {
}
