package com.example.smartmaintenance.controller;

import com.example.smartmaintenance.dto.request.AssignTicketRequest;
import com.example.smartmaintenance.dto.request.UpdateTicketStatusRequest;
import com.example.smartmaintenance.dto.response.TicketResponse;
import com.example.smartmaintenance.enums.TicketPriority;
import com.example.smartmaintenance.enums.TicketStatus;
import com.example.smartmaintenance.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public List<TicketResponse> findAll(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority
    ) {
        return ticketService.findAll(status, priority);
    }

    @GetMapping("/{id}")
    public TicketResponse findById(@PathVariable Long id) {
        return ticketService.findById(id);
    }

    @PutMapping("/{id}/assign")
    public TicketResponse assign(@PathVariable Long id, @Valid @RequestBody AssignTicketRequest request) {
        return ticketService.assignTicket(id, request);
    }

    @PutMapping("/{id}/status")
    public TicketResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateTicketStatusRequest request) {
        return ticketService.updateStatus(id, request);
    }
}