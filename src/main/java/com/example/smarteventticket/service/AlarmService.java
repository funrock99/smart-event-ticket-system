package com.example.smarteventticket.service;

import com.example.smarteventticket.dto.request.BatchEventRequest;
import com.example.smarteventticket.dto.request.CreateEventRequest;
import com.example.smarteventticket.dto.request.SimulationRequest;
import com.example.smarteventticket.dto.response.BatchEventResponse;
import com.example.smarteventticket.dto.response.DedupStatsResponse;
import com.example.smarteventticket.dto.response.EventIngestionResponse;
import com.example.smarteventticket.dto.response.EventResponse;
import com.example.smarteventticket.dto.response.SimulationResultResponse;
import com.example.smarteventticket.entity.AlarmEvent;
import com.example.smarteventticket.entity.MaintenanceTicket;
import com.example.smarteventticket.enums.AlarmSeverity;
import com.example.smarteventticket.enums.EventType;
import com.example.smarteventticket.exception.DuplicateResourceException;
import com.example.smarteventticket.exception.ResourceNotFoundException;
import com.example.smarteventticket.repository.AlarmEventRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlarmService {

    private final AlarmEventRepository alarmEventRepository;
    private final TicketService ticketService;
    private final CacheService cacheService;
    private final DeduplicationService deduplicationService;
    private final IdempotencyService idempotencyService;
    private final RateLimitService rateLimitService;

    public AlarmService(
            AlarmEventRepository alarmEventRepository,
            TicketService ticketService,
            CacheService cacheService,
            DeduplicationService deduplicationService,
            IdempotencyService idempotencyService,
            RateLimitService rateLimitService
    ) {
        this.alarmEventRepository = alarmEventRepository;
        this.ticketService = ticketService;
        this.cacheService = cacheService;
        this.deduplicationService = deduplicationService;
        this.idempotencyService = idempotencyService;
        this.rateLimitService = rateLimitService;
    }

    @Transactional
    public EventProcessingResult createEvent(CreateEventRequest request, String idempotencyKey) {
        String requestHash = buildRequestHash(request);
        IdempotencyService.IdempotencyStartResult idempotencyResult = idempotencyService.startRequest(idempotencyKey, requestHash);
        if (idempotencyResult.replayedResponse().isPresent()) {
            return new EventProcessingResult(HttpStatus.OK, idempotencyResult.replayedResponse().get());
        }
        if (idempotencyResult.processing()) {
            throw new DuplicateResourceException("The same Idempotency-Key is still being processed");
        }
        if (idempotencyResult.failed()) {
            throw new DuplicateResourceException("The same Idempotency-Key is in FAILED state. Retry after TTL expires.");
        }

        try {
            if (!rateLimitService.allow(request.source())) {
                rateLimitService.recordRateLimitedRequest();
                cacheService.evictDashboardSummary();
                EventIngestionResponse response = new EventIngestionResponse(
                        false,
                        null,
                        null,
                        false,
                        true,
                        "Too many requests from this source"
                );
                idempotencyService.markCompleted(idempotencyKey, requestHash, response);
                return new EventProcessingResult(HttpStatus.TOO_MANY_REQUESTS, response);
            }

            if (deduplicationService.isDuplicate(request.source(), request.eventType(), request.businessKey())) {
                Long ticketId = ticketService.findLatestByEvent(request.source(), request.eventType(), request.businessKey())
                        .map(MaintenanceTicket::getId)
                        .orElse(null);
                EventIngestionResponse response = new EventIngestionResponse(
                        true,
                        null,
                        ticketId,
                        true,
                        false,
                        "Duplicated event ignored"
                );
                idempotencyService.markCompleted(idempotencyKey, requestHash, response);
                cacheService.evictDashboardSummary();
                return new EventProcessingResult(HttpStatus.OK, response);
            }

            AlarmEvent event = new AlarmEvent();
            event.setSource(request.source());
            event.setEventType(request.eventType());
            event.setBusinessKey(request.businessKey());
            event.setSeverity(request.severity());
            event.setMessage(request.message());
            event.setPayload(request.payload());
            event.setOccurredAt(LocalDateTime.now());

            AlarmEvent savedEvent = alarmEventRepository.save(event);
            MaintenanceTicket ticket = ticketService.createTicketFromEvent(savedEvent);
            EventIngestionResponse response = new EventIngestionResponse(
                    true,
                    savedEvent.getId(),
                    ticket.getId(),
                    false,
                    false,
                    "Event accepted and ticket created"
            );
            idempotencyService.markCompleted(idempotencyKey, requestHash, response);
            cacheService.evictDashboardSummary();
            return new EventProcessingResult(HttpStatus.CREATED, response);
        } catch (RuntimeException ex) {
            idempotencyService.markFailed(idempotencyKey, requestHash, ex.getClass().getSimpleName());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> findAll(String source, EventType eventType, AlarmSeverity severity, Pageable pageable) {
        return alarmEventRepository.search(source, eventType, severity, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse findById(Long id) {
        AlarmEvent event = alarmEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + id));
        return toResponse(event);
    }

    @Transactional
    public BatchEventResponse createBatch(BatchEventRequest request) {
        List<EventIngestionResponse> results = request.events()
                .stream()
                .map(event -> createEvent(event, null).response())
                .toList();

        int created = (int) results.stream().filter(result -> result.eventId() != null).count();
        int duplicated = (int) results.stream().filter(EventIngestionResponse::duplicated).count();
        int rateLimited = (int) results.stream().filter(EventIngestionResponse::rateLimited).count();
        return new BatchEventResponse(results.size(), created, duplicated, rateLimited, results);
    }

    @Transactional
    public SimulationResultResponse simulate(SimulationRequest request) {
        int created = 0;
        int duplicated = 0;
        int rateLimited = 0;

        for (int i = 0; i < request.count(); i++) {
            String businessKey = request.duplicateBusinessKey()
                    ? request.businessKeyPrefix()
                    : request.businessKeyPrefix() + "-" + (i + 1);
            CreateEventRequest eventRequest = new CreateEventRequest(
                    request.source(),
                    request.eventType(),
                    businessKey,
                    request.severity(),
                    request.message(),
                    request.payload()
            );
            EventIngestionResponse response = createEvent(eventRequest, null).response();
            if (response.eventId() != null) {
                created++;
            } else if (response.duplicated()) {
                duplicated++;
            } else if (response.rateLimited()) {
                rateLimited++;
            }
        }

        return new SimulationResultResponse(request.source(), request.count(), created, duplicated, rateLimited);
    }

    @Transactional(readOnly = true)
    public DedupStatsResponse getDedupStats() {
        return deduplicationService.getStats();
    }

    private String buildRequestHash(CreateEventRequest request) {
        String canonicalRequest = "{" +
                "\"businessKey\":\"" + escapeJson(request.businessKey()) + "\"," +
                "\"eventType\":\"" + request.eventType().name() + "\"," +
                "\"message\":\"" + escapeJson(request.message()) + "\"," +
                "\"payload\":\"" + escapeJson(request.payload() == null ? "" : request.payload()) + "\"," +
                "\"severity\":\"" + request.severity().name() + "\"," +
                "\"source\":\"" + escapeJson(request.source()) + "\"" +
                "}";
        return sha256(canonicalRequest);
    }

    private String sha256(String value) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(messageDigest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash request body", ex);
        }
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private EventResponse toResponse(AlarmEvent event) {
        return new EventResponse(
                event.getId(),
                event.getSource(),
                event.getEventType(),
                event.getBusinessKey(),
                event.getSeverity(),
                event.getMessage(),
                event.getPayload(),
                event.getOccurredAt(),
                event.getCreatedAt()
        );
    }

    public record EventProcessingResult(HttpStatus status, EventIngestionResponse response) {
    }
}

