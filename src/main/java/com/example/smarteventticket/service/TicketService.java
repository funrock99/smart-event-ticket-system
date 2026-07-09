package com.example.smarteventticket.service;

import com.example.smarteventticket.dto.request.AssignTicketRequest;
import com.example.smarteventticket.dto.request.UpdateTicketStatusRequest;
import com.example.smarteventticket.dto.response.TicketResponse;
import com.example.smarteventticket.dto.response.TicketStatusHistoryResponse;
import com.example.smarteventticket.entity.AlarmEvent;
import com.example.smarteventticket.entity.MaintenanceTicket;
import com.example.smarteventticket.entity.TicketStatusHistory;
import com.example.smarteventticket.enums.EventType;
import com.example.smarteventticket.enums.TicketPriority;
import com.example.smarteventticket.enums.TicketStatus;
import com.example.smarteventticket.exception.InvalidStatusTransitionException;
import com.example.smarteventticket.exception.ResourceNotFoundException;
import com.example.smarteventticket.repository.MaintenanceTicketRepository;
import com.example.smarteventticket.repository.TicketStatusHistoryRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MaintenanceTicketRepository ticketRepository;
    private final TicketStatusHistoryRepository ticketStatusHistoryRepository;
    private final CacheService cacheService;

    public TicketService(
            MaintenanceTicketRepository ticketRepository,
            TicketStatusHistoryRepository ticketStatusHistoryRepository,
            CacheService cacheService
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketStatusHistoryRepository = ticketStatusHistoryRepository;
        this.cacheService = cacheService;
    }

    @Transactional
    public MaintenanceTicket createTicketFromEvent(AlarmEvent event) {
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setTicketNo(generateTicketNo());
        ticket.setSource(event.getSource());
        ticket.setEventType(event.getEventType());
        ticket.setBusinessKey(event.getBusinessKey());
        TicketPriority priority = mapPriority(event);
        ticket.setPriority(priority);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setDescription(event.getMessage());
        ticket.setSlaDueAt(resolveSlaDueAt(priority));
        MaintenanceTicket savedTicket = ticketRepository.save(ticket);
        saveStatusHistory(savedTicket, null, TicketStatus.OPEN, "system");
        cacheService.evictDashboardSummary();
        return savedTicket;
    }

    @Transactional(readOnly = true)
    public Page<TicketResponse> findAll(TicketStatus status, TicketPriority priority, Pageable pageable) {
        return ticketRepository.search(status, priority, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TicketResponse findById(Long id) {
        return toResponse(getTicketEntity(id));
    }

    @Transactional(readOnly = true)
    public List<TicketStatusHistoryResponse> findHistory(Long ticketId) {
        getTicketEntity(ticketId);
        return ticketStatusHistoryRepository.findByTicketIdOrderByChangedAtDesc(ticketId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
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
        TicketStatus currentStatus = ticket.getStatus();
        validateStatusTransition(currentStatus, request.status());

        ticket.setStatus(request.status());
        if (request.status() == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        if (request.status() == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
        }

        MaintenanceTicket savedTicket = ticketRepository.save(ticket);
        saveStatusHistory(savedTicket, currentStatus, request.status(), resolveChangedBy(savedTicket));
        TicketResponse response = toResponse(savedTicket);
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
        String datePart = LocalDateTime.now().format(DATE_FORMATTER);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "EVT-" + datePart + "-" + suffix;
    }

    private TicketPriority mapPriority(AlarmEvent event) {
        return switch (event.getSeverity()) {
            case LOW -> TicketPriority.LOW;
            case MEDIUM -> TicketPriority.MEDIUM;
            case HIGH -> TicketPriority.HIGH;
            case CRITICAL -> TicketPriority.URGENT;
        };
    }

    private LocalDateTime resolveSlaDueAt(TicketPriority priority) {
        LocalDateTime now = LocalDateTime.now();
        return switch (priority) {
            case URGENT -> now.plusHours(1);
            case HIGH -> now.plusHours(4);
            case MEDIUM -> now.plusHours(8);
            case LOW -> now.plusHours(24);
        };
    }

    private boolean isSlaBreached(MaintenanceTicket ticket) {
        if (ticket.getSlaDueAt() == null) {
            return false;
        }
        LocalDateTime effectiveTime = ticket.getClosedAt();
        if (effectiveTime == null) {
            effectiveTime = ticket.getResolvedAt();
        }
        if (effectiveTime == null) {
            effectiveTime = LocalDateTime.now();
        }
        return effectiveTime.isAfter(ticket.getSlaDueAt());
    }

    private String resolveChangedBy(MaintenanceTicket ticket) {
        if (ticket.getAssignee() == null || ticket.getAssignee().isBlank()) {
            return "system";
        }
        return ticket.getAssignee();
    }

    private void saveStatusHistory(
            MaintenanceTicket ticket,
            TicketStatus fromStatus,
            TicketStatus toStatus,
            String changedBy
    ) {
        TicketStatusHistory history = new TicketStatusHistory();
        history.setTicketId(ticket.getId());
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setChangedBy(changedBy);
        ticketStatusHistoryRepository.save(history);
    }

    private TicketStatusHistoryResponse toHistoryResponse(TicketStatusHistory history) {
        return new TicketStatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy(),
                history.getChangedAt()
        );
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
                ticket.getClosedAt(),
                ticket.getSlaDueAt(),
                isSlaBreached(ticket)
        );
    }
}
