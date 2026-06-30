package com.example.smartmaintenance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignTicketRequest(
        @NotBlank @Size(min = 1, max = 50) String assignee
) {
}

