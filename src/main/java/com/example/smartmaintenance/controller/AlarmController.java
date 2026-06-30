package com.example.smartmaintenance.controller;

import com.example.smartmaintenance.dto.request.ReportAlarmRequest;
import com.example.smartmaintenance.dto.response.AlarmEventResponse;
import com.example.smartmaintenance.dto.response.AlarmReportResponse;
import com.example.smartmaintenance.enums.AlarmSeverity;
import com.example.smartmaintenance.service.AlarmService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alarms")
public class AlarmController {

    private final AlarmService alarmService;

    public AlarmController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlarmReportResponse report(@Valid @RequestBody ReportAlarmRequest request) {
        return alarmService.reportAlarm(request);
    }

    @GetMapping
    public List<AlarmEventResponse> findAll(
            @RequestParam(required = false) String equipmentId,
            @RequestParam(required = false) AlarmSeverity severity
    ) {
        return alarmService.findAll(equipmentId, severity);
    }

    @GetMapping("/recent")
    public List<AlarmEventResponse> findRecent() {
        return alarmService.findRecent();
    }
}
