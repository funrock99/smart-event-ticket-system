package com.example.smartmaintenance.service;

import com.example.smartmaintenance.dto.request.ReportAlarmRequest;
import com.example.smartmaintenance.dto.response.AlarmEventResponse;
import com.example.smartmaintenance.dto.response.AlarmReportResponse;
import com.example.smartmaintenance.entity.AlarmEvent;
import com.example.smartmaintenance.entity.MaintenanceTicket;
import com.example.smartmaintenance.enums.AlarmSeverity;
import com.example.smartmaintenance.enums.EquipmentStatus;
import com.example.smartmaintenance.exception.DuplicateResourceException;
import com.example.smartmaintenance.repository.AlarmEventRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlarmService {

    private final AlarmEventRepository alarmEventRepository;
    private final EquipmentService equipmentService;
    private final TicketService ticketService;
    private final CacheService cacheService;

    public AlarmService(
            AlarmEventRepository alarmEventRepository,
            EquipmentService equipmentService,
            TicketService ticketService,
            CacheService cacheService
    ) {
        this.alarmEventRepository = alarmEventRepository;
        this.equipmentService = equipmentService;
        this.ticketService = ticketService;
        this.cacheService = cacheService;
    }

    @Transactional
    public AlarmReportResponse reportAlarm(ReportAlarmRequest request) {
        equipmentService.getEquipmentEntity(request.equipmentId());
        boolean acquired = cacheService.acquireAlarmDedupKey(request.equipmentId(), request.alarmCode());
        if (!acquired) {
            throw new DuplicateResourceException(
                    "Duplicate alarm suppressed within dedup window: "
                            + request.equipmentId() + " / " + request.alarmCode()
            );
        }

        AlarmEvent alarmEvent = new AlarmEvent();
        alarmEvent.setEquipmentId(request.equipmentId());
        alarmEvent.setAlarmCode(request.alarmCode());
        alarmEvent.setSeverity(request.severity());
        alarmEvent.setMessage(request.message());
        alarmEvent.setOccurredAt(LocalDateTime.now());

        AlarmEvent savedAlarm = alarmEventRepository.save(alarmEvent);
        equipmentService.updateEquipmentStatus(request.equipmentId(), EquipmentStatus.DOWN);
        MaintenanceTicket ticket = ticketService.createTicketFromAlarm(savedAlarm);
        cacheService.cacheRecentAlarm(toResponse(savedAlarm));

        return new AlarmReportResponse(
                savedAlarm.getId(),
                savedAlarm.getEquipmentId(),
                savedAlarm.getAlarmCode(),
                savedAlarm.getSeverity(),
                ticket.getTicketNo(),
                "Alarm received and maintenance ticket created"
        );
    }

    @Transactional(readOnly = true)
    public List<AlarmEventResponse> findAll(String equipmentId, AlarmSeverity severity) {
        return alarmEventRepository.findAll()
                .stream()
                .filter(alarm -> equipmentId == null || alarm.getEquipmentId().equalsIgnoreCase(equipmentId))
                .filter(alarm -> severity == null || alarm.getSeverity() == severity)
                .sorted(Comparator.comparing(AlarmEvent::getOccurredAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlarmEventResponse> findRecent() {
        List<AlarmEventResponse> cachedAlarms = cacheService.getRecentAlarms();
        if (!cachedAlarms.isEmpty()) {
            return cachedAlarms;
        }

        return alarmEventRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(AlarmEvent::getOccurredAt).reversed())
                .limit(20)
                .map(this::toResponse)
                .toList();
    }

    AlarmEventResponse toResponse(AlarmEvent alarmEvent) {
        return new AlarmEventResponse(
                alarmEvent.getId(),
                alarmEvent.getEquipmentId(),
                alarmEvent.getAlarmCode(),
                alarmEvent.getSeverity(),
                alarmEvent.getMessage(),
                alarmEvent.getOccurredAt(),
                alarmEvent.getCreatedAt()
        );
    }
}
