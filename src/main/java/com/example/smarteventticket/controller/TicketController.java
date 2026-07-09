package com.example.smarteventticket.controller;

import com.example.smarteventticket.dto.request.AssignTicketRequest;
import com.example.smarteventticket.dto.request.UpdateTicketStatusRequest;
import com.example.smarteventticket.dto.response.PagedResponse;
import com.example.smarteventticket.dto.response.TicketResponse;
import com.example.smarteventticket.dto.response.TicketStatusHistoryResponse;
import com.example.smarteventticket.enums.TicketPriority;
import com.example.smarteventticket.enums.TicketStatus;
import com.example.smarteventticket.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    public PagedResponse<TicketResponse> findAll(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return PagedResponse.from(ticketService.findAll(status, priority, pageable));
    }

    @GetMapping("/{id}")
    public TicketResponse findById(@PathVariable Long id) {
        return ticketService.findById(id);
    }

    @GetMapping("/{id}/history")
    public List<TicketStatusHistoryResponse> findHistory(@PathVariable Long id) {
        return ticketService.findHistory(id);
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
