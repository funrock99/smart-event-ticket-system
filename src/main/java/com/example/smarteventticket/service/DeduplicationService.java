package com.example.smarteventticket.service;

import com.example.smarteventticket.dto.response.DedupStatsResponse;
import com.example.smarteventticket.enums.EventType;
import com.example.smarteventticket.repository.AlarmEventRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class DeduplicationService {

    private final CacheService cacheService;
    private final AlarmEventRepository alarmEventRepository;

    public DeduplicationService(CacheService cacheService, AlarmEventRepository alarmEventRepository) {
        this.cacheService = cacheService;
        this.alarmEventRepository = alarmEventRepository;
    }

    public boolean isDuplicate(String source, EventType eventType, String businessKey) {
        boolean acquired = cacheService.acquireAlarmDedupKey(source, eventType.name(), businessKey);
        if (!acquired) {
            cacheService.incrementDuplicateEventCount();
        }
        return !acquired;
    }

    public void rememberTicket(String source, EventType eventType, String businessKey, Long ticketId) {
        cacheService.updateAlarmDedupTicketId(source, eventType.name(), businessKey, ticketId);
    }

    public Optional<Long> getDuplicateTicketId(String source, EventType eventType, String businessKey) {
        return cacheService.getAlarmDedupTicketId(source, eventType.name(), businessKey);
    }

    public DedupStatsResponse getStats() {
        long validEvents = alarmEventRepository.count();
        long duplicatedEvents = cacheService.getDuplicateEventCount();
        long totalProcessedEvents = validEvents + duplicatedEvents;
        double dedupRate = totalProcessedEvents == 0 ? 0.0 : (double) duplicatedEvents / totalProcessedEvents;
        return new DedupStatsResponse(totalProcessedEvents, validEvents, duplicatedEvents, dedupRate);
    }
}
