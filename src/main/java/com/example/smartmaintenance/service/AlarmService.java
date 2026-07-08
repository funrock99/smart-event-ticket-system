package com.example.smartmaintenance.service;

import com.example.smartmaintenance.dto.request.BatchEventRequest;
import com.example.smartmaintenance.dto.request.CreateEventRequest;
import com.example.smartmaintenance.dto.request.SimulationRequest;
import com.example.smartmaintenance.dto.response.BatchEventResponse;
import com.example.smartmaintenance.dto.response.DedupStatsResponse;
import com.example.smartmaintenance.dto.response.EventIngestionResponse;
import com.example.smartmaintenance.dto.response.EventResponse;
import com.example.smartmaintenance.dto.response.SimulationResultResponse;
import com.example.smartmaintenance.entity.AlarmEvent;
import com.example.smartmaintenance.entity.MaintenanceTicket;
import com.example.smartmaintenance.enums.AlarmSeverity;
import com.example.smartmaintenance.enums.EventType;
import com.example.smartmaintenance.exception.ResourceNotFoundException;
import com.example.smartmaintenance.repository.AlarmEventRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
        Optional<EventIngestionResponse> cachedResult = idempotencyService.getProcessedResult(idempotencyKey, requestHash);
        if (cachedResult.isPresent()) {
            return new EventProcessingResult(HttpStatus.OK, cachedResult.get());
        }

        if (!rateLimitService.allow(request.source())) {
            rateLimitService.recordRateLimitedRequest();
            cacheService.evictDashboardSummary();
            return new EventProcessingResult(
                    HttpStatus.TOO_MANY_REQUESTS,
                    new EventIngestionResponse(false, null, null, false, true, "Too many requests from this source")
            );
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
            idempotencyService.saveProcessedResult(idempotencyKey, requestHash, response);
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
        idempotencyService.saveProcessedResult(idempotencyKey, requestHash, response);
        cacheService.evictDashboardSummary();
        return new EventProcessingResult(HttpStatus.CREATED, response);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> findAll(String source, EventType eventType, AlarmSeverity severity) {
        return alarmEventRepository.findAll()
                .stream()
                .filter(event -> source == null || event.getSource().equalsIgnoreCase(source))
                .filter(event -> eventType == null || event.getEventType() == eventType)
                .filter(event -> severity == null || event.getSeverity() == severity)
                .sorted(Comparator.comparing(AlarmEvent::getOccurredAt).reversed())
                .map(this::toResponse)
                .toList();
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
        return String.join(
                "|",
                request.source(),
                request.eventType().name(),
                request.businessKey(),
                request.severity().name(),
                request.message(),
                request.payload() == null ? "" : request.payload()
        );
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