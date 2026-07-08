package com.example.smartmaintenance.service;

import com.example.smartmaintenance.dto.request.AssignTicketRequest;
import com.example.smartmaintenance.dto.request.UpdateTicketStatusRequest;
import com.example.smartmaintenance.dto.response.TicketResponse;
import com.example.smartmaintenance.entity.AlarmEvent;
import com.example.smartmaintenance.entity.MaintenanceTicket;
import com.example.smartmaintenance.enums.EventType;
import com.example.smartmaintenance.enums.TicketPriority;
import com.example.smartmaintenance.enums.TicketStatus;
import com.example.smartmaintenance.exception.InvalidStatusTransitionException;
import com.example.smartmaintenance.exception.ResourceNotFoundException;
import com.example.smartmaintenance.repository.MaintenanceTicketRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MaintenanceTicketRepository ticketRepository;
    private final CacheService cacheService;

    public TicketService(MaintenanceTicketRepository ticketRepository, CacheService cacheService) {
        this.ticketRepository = ticketRepository;
        this.cacheService = cacheService;
    }

    @Transactional
    public MaintenanceTicket createTicketFromEvent(AlarmEvent event) {
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setTicketNo(generateTicketNo());
        ticket.setSource(event.getSource());
        ticket.setEventType(event.getEventType());
        ticket.setBusinessKey(event.getBusinessKey());
        ticket.setPriority(mapPriority(event));
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setDescription(event.getMessage());
        MaintenanceTicket savedTicket = ticketRepository.save(ticket);
        cacheService.evictDashboardSummary();
        return savedTicket;
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> findAll(TicketStatus status, TicketPriority priority) {
        return ticketRepository.findAll()
                .stream()
                .filter(ticket -> status == null || ticket.getStatus() == status)
                .filter(ticket -> priority == null || ticket.getPriority() == priority)
                .sorted(Comparator.comparing(MaintenanceTicket::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse findById(Long id) {
        return toResponse(getTicketEntity(id));
    }

    @Transactional
    public TicketResponse assignTicket(Long id, AssignTicketRequest request) {
        MaintenanceTicket ticket = getTicketEntity(id);
        ticket.setAssignee(request.assignee());
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse updateStatus(Long id, UpdateTicketStatusRequest request) {
        MaintenanceTicket ticket = getTicketEntity(id);
        validateStatusTransition(ticket.getStatus(), request.status());

        ticket.setStatus(request.status());
        if (request.status() == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        if (request.status() == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
        }

        TicketResponse response = toResponse(ticketRepository.save(ticket));
        cacheService.evictDashboardSummary();
        return response;
    }

    @Transactional(readOnly = true)
    public Optional<MaintenanceTicket> findLatestByEvent(String source, EventType eventType, String businessKey) {
        return ticketRepository.findTopBySourceAndEventTypeAndBusinessKeyOrderByCreatedAtDesc(source, eventType, businessKey);
    }

    @Transactional(readOnly = true)
    public MaintenanceTicket getTicketEntity(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + id));
    }

    private void validateStatusTransition(TicketStatus current, TicketStatus next) {
        boolean valid = switch (current) {
            case OPEN -> next == TicketStatus.PROCESSING;
            case PROCESSING -> next == TicketStatus.RESOLVED;
            case RESOLVED -> next == TicketStatus.CLOSED;
            case CLOSED -> false;
        };

        if (!valid) {
            throw new InvalidStatusTransitionException(
                    "Invalid ticket status transition: " + current + " -> " + next
            );
        }
    }

    private String generateTicketNo() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        long sequence = ticketRepository.countByCreatedAtBetween(start, end) + 1;
        return "EVT-" + today.format(DATE_FORMATTER) + "-" + String.format("%04d", sequence);
    }

    private TicketPriority mapPriority(AlarmEvent event) {
        return switch (event.getSeverity()) {
            case LOW -> TicketPriority.LOW;
            case MEDIUM -> TicketPriority.MEDIUM;
            case HIGH -> TicketPriority.HIGH;
            case CRITICAL -> TicketPriority.URGENT;
        };
    }

    private TicketResponse toResponse(MaintenanceTicket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getSource(),
                ticket.getEventType(),
                ticket.getBusinessKey(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAssignee(),
                ticket.getDescription(),
                ticket.getCreatedAt(),
                ticket.getResolvedAt(),
                ticket.getClosedAt()
        );
    }
}