package com.example.smarteventticket.dto.response;

import com.example.smarteventticket.enums.TicketStatus;
import java.time.LocalDateTime;

public record TicketStatusHistoryResponse(
        Long id,
        TicketStatus fromStatus,
        TicketStatus toStatus,
        String changedBy,
        LocalDateTime changedAt
) {
}
