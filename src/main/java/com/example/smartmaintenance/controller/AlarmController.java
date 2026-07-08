package com.example.smartmaintenance.controller;

import com.example.smartmaintenance.dto.request.BatchEventRequest;
import com.example.smartmaintenance.dto.request.CreateEventRequest;
import com.example.smartmaintenance.dto.request.SimulationRequest;
import com.example.smartmaintenance.dto.response.BatchEventResponse;
import com.example.smartmaintenance.dto.response.DedupStatsResponse;
import com.example.smartmaintenance.dto.response.EventIngestionResponse;
import com.example.smartmaintenance.dto.response.EventResponse;
import com.example.smartmaintenance.dto.response.SimulationResultResponse;
import com.example.smartmaintenance.enums.AlarmSeverity;
import com.example.smartmaintenance.enums.EventType;
import com.example.smartmaintenance.service.AlarmService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class AlarmController {

    private final AlarmService alarmService;

    public AlarmController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    @PostMapping
    public ResponseEntity<EventIngestionResponse> create(
            @Valid @RequestBody CreateEventRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        AlarmService.EventProcessingResult result = alarmService.createEvent(request, idempotencyKey);
        return ResponseEntity.status(result.status()).body(result.response());
    }

    @GetMapping
    public List<EventResponse> findAll(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) EventType eventType,
            @RequestParam(required = false) AlarmSeverity severity
    ) {
        return alarmService.findAll(source, eventType, severity);
    }

    @GetMapping("/{id}")
    public EventResponse findById(@PathVariable Long id) {
        return alarmService.findById(id);
    }

    @PostMapping("/batch")
    public ResponseEntity<BatchEventResponse> createBatch(@Valid @RequestBody BatchEventRequest request) {
        return ResponseEntity.ok(alarmService.createBatch(request));
    }

    @PostMapping("/simulate")
    public ResponseEntity<SimulationResultResponse> simulate(@Valid @RequestBody SimulationRequest request) {
        return ResponseEntity.ok(alarmService.simulate(request));
    }

    @GetMapping("/dedup-stats")
    public DedupStatsResponse getDedupStats() {
        return alarmService.getDedupStats();
    }
}