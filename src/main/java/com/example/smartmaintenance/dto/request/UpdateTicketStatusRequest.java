package com.example.smartmaintenance.dto.request;

import com.example.smartmaintenance.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(
        @NotNull TicketStatus status
) {
}

