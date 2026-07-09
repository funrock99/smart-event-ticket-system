package com.example.smarteventticket.service;

import com.example.smarteventticket.dto.request.CreateEventRequest;
import com.example.smarteventticket.dto.response.EventIngestionResponse;
import com.example.smarteventticket.entity.AlarmEvent;
import com.example.smarteventticket.entity.MaintenanceTicket;
import com.example.smarteventticket.enums.AlarmSeverity;
import com.example.smarteventticket.enums.EventType;
import com.example.smarteventticket.repository.AlarmEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlarmServiceTest {

    @Mock
    private AlarmEventRepository alarmEventRepository;

    @Mock
    private TicketService ticketService;

    @Mock
    private CacheService cacheService;

    @Mock
    private DeduplicationService deduplicationService;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private RateLimitService rateLimitService;

    @InjectMocks
    private AlarmService alarmService;

    @Test
    void createEventShouldCreateEventAndTicket() {
        CreateEventRequest request = new CreateEventRequest(
                "payment-system",
                EventType.TRANSACTION_ERROR,
                "TXN-001",
                AlarmSeverity.HIGH,
                "Transaction failed",
                "{}"
        );
        AlarmEvent savedEvent = new AlarmEvent();
        savedEvent.setId(1L);
        savedEvent.setSource(request.source());
        savedEvent.setEventType(request.eventType());
        savedEvent.setBusinessKey(request.businessKey());
        savedEvent.setSeverity(request.severity());
        savedEvent.setMessage(request.message());

        MaintenanceTicket ticket = new MaintenanceTicket();
        ticket.setId(55L);

        when(idempotencyService.startRequest(any(), any())).thenReturn(IdempotencyService.IdempotencyStartResult.startNew());
        when(rateLimitService.allow("payment-system")).thenReturn(true);
        when(deduplicationService.isDuplicate("payment-system", EventType.TRANSACTION_ERROR, "TXN-001")).thenReturn(false);
        when(alarmEventRepository.save(any(AlarmEvent.class))).thenReturn(savedEvent);
        when(ticketService.createTicketFromEvent(savedEvent)).thenReturn(ticket);

        AlarmService.EventProcessingResult result = alarmService.createEvent(request, "idem-1");

        assertEquals(HttpStatus.CREATED, result.status());
        assertEquals(1L, result.response().eventId());
        assertEquals(55L, result.response().ticketId());
        verify(idempotencyService).markCompleted(any(), any(), any(EventIngestionResponse.class));
    }

    @Test
    void createEventShouldReturnCachedIdempotentResponse() {
        CreateEventRequest request = new CreateEventRequest(
                "payment-system",
                EventType.TRANSACTION_ERROR,
                "TXN-001",
                AlarmSeverity.HIGH,
                "Transaction failed",
                "{}"
        );
        EventIngestionResponse cached = new EventIngestionResponse(true, 1L, 55L, false, false, "cached");
        when(idempotencyService.startRequest(any(), any()))
                .thenReturn(IdempotencyService.IdempotencyStartResult.replay(cached));

        AlarmService.EventProcessingResult result = alarmService.createEvent(request, "idem-1");

        assertEquals(HttpStatus.OK, result.status());
        assertEquals(cached, result.response());
        verify(alarmEventRepository, never()).save(any());
    }

    @Test
    void createEventShouldReturnRateLimitedResponse() {
        CreateEventRequest request = new CreateEventRequest(
                "payment-system",
                EventType.TRANSACTION_ERROR,
                "TXN-001",
                AlarmSeverity.HIGH,
                "Transaction failed",
                "{}"
        );
        when(idempotencyService.startRequest(any(), any())).thenReturn(IdempotencyService.IdempotencyStartResult.startNew());
        when(rateLimitService.allow("payment-system")).thenReturn(false);

        AlarmService.EventProcessingResult result = alarmService.createEvent(request, null);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, result.status());
        assertTrue(result.response().rateLimited());
        verify(rateLimitService).recordRateLimitedRequest();
    }
}

