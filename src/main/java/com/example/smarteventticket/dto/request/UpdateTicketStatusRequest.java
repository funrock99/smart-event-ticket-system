package com.example.smarteventticket.dto.request;

import com.example.smarteventticket.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(
        @NotNull TicketStatus status
) {
}


