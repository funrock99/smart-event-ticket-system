package com.example.smartmaintenance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.smartmaintenance.dto.request.ReportAlarmRequest;
import com.example.smartmaintenance.dto.response.AlarmEventResponse;
import com.example.smartmaintenance.entity.AlarmEvent;
import com.example.smartmaintenance.entity.Equipment;
import com.example.smartmaintenance.entity.MaintenanceTicket;
import com.example.smartmaintenance.enums.AlarmSeverity;
import com.example.smartmaintenance.enums.EquipmentStatus;
import com.example.smartmaintenance.exception.DuplicateResourceException;
import com.example.smartmaintenance.repository.AlarmEventRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlarmServiceTest {

    @Mock
    private AlarmEventRepository alarmEventRepository;

    @Mock
    private EquipmentService equipmentService;

    @Mock
    private TicketService ticketService;

    @Mock
    private CacheService cacheService;

    @InjectMocks
    private AlarmService alarmService;

    @Test
    void reportAlarmShouldCreateAlarmAndTicketAndUpdateEquipmentStatus() {
        ReportAlarmRequest request = new ReportAlarmRequest("EQP-001", "TEMP_HIGH", AlarmSeverity.HIGH, "Overheat");
        Equipment equipment = new Equipment();
        equipment.setEquipmentId("EQP-001");
        AlarmEvent savedAlarm = new AlarmEvent();
        savedAlarm.setId(1L);
        savedAlarm.setEquipmentId(request.equipmentId());
        savedAlarm.setAlarmCode(request.alarmCode());
        savedAlarm.setSeverity(request.severity());
        savedAlarm.setMessage(request.message());
        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setTicketNo("MT-20260628-0001");

        when(equipmentService.getEquipmentEntity("EQP-001")).thenReturn(equipment);
        when(cacheService.acquireAlarmDedupKey("EQP-001", "TEMP_HIGH")).thenReturn(true);
        when(alarmEventRepository.save(any(AlarmEvent.class))).thenReturn(savedAlarm);
        when(ticketService.createTicketFromAlarm(savedAlarm)).thenReturn(ticket);

        var response = alarmService.reportAlarm(request);

        assertEquals("MT-20260628-0001", response.ticketNo());
        verify(equipmentService).updateEquipmentStatus("EQP-001", EquipmentStatus.DOWN);
        verify(ticketService).createTicketFromAlarm(savedAlarm);
        verify(cacheService).cacheRecentAlarm(any());
    }

    @Test
    void reportAlarmShouldRejectDuplicateAlarmWithinDedupWindow() {
        ReportAlarmRequest request = new ReportAlarmRequest("EQP-001", "TEMP_HIGH", AlarmSeverity.HIGH, "Overheat");
        Equipment equipment = new Equipment();
        equipment.setEquipmentId("EQP-001");

        when(equipmentService.getEquipmentEntity("EQP-001")).thenReturn(equipment);
        when(cacheService.acquireAlarmDedupKey("EQP-001", "TEMP_HIGH")).thenReturn(false);

        assertThrows(DuplicateResourceException.class, () -> alarmService.reportAlarm(request));
    }

    @Test
    void findRecentShouldReturnCachedAlarmsWhenAvailable() {
        AlarmEventResponse cachedAlarm = new AlarmEventResponse(
                1L,
                "EQP-001",
                "TEMP_HIGH",
                AlarmSeverity.HIGH,
                "Overheat",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        when(cacheService.getRecentAlarms()).thenReturn(List.of(cachedAlarm));

        var response = alarmService.findRecent();

        assertEquals(1, response.size());
        assertEquals("EQP-001", response.get(0).equipmentId());
    }

    @Test
    void findRecentShouldFallbackToRepositoryWhenCacheIsEmpty() {
        AlarmEvent alarmEvent = new AlarmEvent();
        alarmEvent.setId(2L);
        alarmEvent.setEquipmentId("EQP-002");
        alarmEvent.setAlarmCode("PRESSURE_HIGH");
        alarmEvent.setSeverity(AlarmSeverity.CRITICAL);
        alarmEvent.setMessage("Pressure exceeded threshold");
        alarmEvent.setOccurredAt(LocalDateTime.now());
        alarmEvent.setCreatedAt(LocalDateTime.now());

        when(cacheService.getRecentAlarms()).thenReturn(List.of());
        when(alarmEventRepository.findAll()).thenReturn(List.of(alarmEvent));

        var response = alarmService.findRecent();

        assertEquals(1, response.size());
        assertEquals("EQP-002", response.get(0).equipmentId());
    }
}

