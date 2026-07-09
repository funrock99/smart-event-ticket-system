package com.example.smarteventticket.controller;

import com.example.smarteventticket.dto.request.BatchEventRequest;
import com.example.smarteventticket.dto.request.CreateEventRequest;
import com.example.smarteventticket.dto.request.SimulationRequest;
import com.example.smarteventticket.dto.response.BatchEventResponse;
import com.example.smarteventticket.dto.response.DedupStatsResponse;
import com.example.smarteventticket.dto.response.EventIngestionResponse;
import com.example.smarteventticket.dto.response.EventResponse;
import com.example.smarteventticket.dto.response.PagedResponse;
import com.example.smarteventticket.dto.response.SimulationResultResponse;
import com.example.smarteventticket.enums.AlarmSeverity;
import com.example.smarteventticket.enums.EventType;
import com.example.smarteventticket.service.AlarmService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    public PagedResponse<EventResponse> findAll(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) EventType eventType,
            @RequestParam(required = false) AlarmSeverity severity,
            @PageableDefault(size = 20, sort = "occurredAt") Pageable pageable
    ) {
        return PagedResponse.from(alarmService.findAll(source, eventType, severity, pageable));
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
