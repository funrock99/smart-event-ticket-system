package com.example.smartmaintenance.service;

import com.example.smartmaintenance.dto.request.AssignTicketRequest;
import com.example.smartmaintenance.dto.request.UpdateTicketStatusRequest;
import com.example.smartmaintenance.dto.response.TicketResponse;
import com.example.smartmaintenance.entity.AlarmEvent;
import com.example.smartmaintenance.entity.MaintenanceTicket;
import com.example.smartmaintenance.enums.EquipmentStatus;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MaintenanceTicketRepository ticketRepository;
    private final EquipmentService equipmentService;
    private final CacheService cacheService;

    public TicketService(
            MaintenanceTicketRepository ticketRepository,
            EquipmentService equipmentService,
            CacheService cacheService
    ) {
        this.ticketRepository = ticketRepository;
        this.equipmentService = equipmentService;
        this.cacheService = cacheService;
    }

    @Transactional
    public MaintenanceTicket createTicketFromAlarm(AlarmEvent alarmEvent) {
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setTicketNo(generateTicketNo());
        ticket.setEquipmentId(alarmEvent.getEquipmentId());
        ticket.setAlarmCode(alarmEvent.getAlarmCode());
        ticket.setPriority(mapPriority(alarmEvent));
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setDescription(alarmEvent.getMessage());
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
    public TicketResponse findByTicketNo(String ticketNo) {
        return toResponse(getTicketEntity(ticketNo));
    }

    @Transactional
    public TicketResponse assignTicket(String ticketNo, AssignTicketRequest request) {
        MaintenanceTicket ticket = getTicketEntity(ticketNo);
        ticket.setAssignee(request.assignee());
        return toResponse(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse updateStatus(String ticketNo, UpdateTicketStatusRequest request) {
        MaintenanceTicket ticket = getTicketEntity(ticketNo);
        validateStatusTransition(ticket.getStatus(), request.status());

        ticket.setStatus(request.status());
        if (request.status() == TicketStatus.IN_PROGRESS) {
            equipmentService.updateEquipmentStatus(ticket.getEquipmentId(), EquipmentStatus.MAINTENANCE);
        }
        if (request.status() == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        if (request.status() == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
            equipmentService.updateEquipmentStatus(ticket.getEquipmentId(), EquipmentStatus.RUNNING);
        }

        TicketResponse response = toResponse(ticketRepository.save(ticket));
        cacheService.evictDashboardSummary();
        return response;
    }

    @Transactional(readOnly = true)
    public MaintenanceTicket getTicketEntity(String ticketNo) {
        return ticketRepository.findByTicketNo(ticketNo)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketNo));
    }

    private void validateStatusTransition(TicketStatus current, TicketStatus next) {
        boolean valid = switch (current) {
            case OPEN -> next == TicketStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == TicketStatus.RESOLVED;
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
        return "MT-" + today.format(DATE_FORMATTER) + "-" + String.format("%04d", sequence);
    }

    private TicketPriority mapPriority(AlarmEvent alarmEvent) {
        return switch (alarmEvent.getSeverity()) {
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
                ticket.getEquipmentId(),
                ticket.getAlarmCode(),
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
