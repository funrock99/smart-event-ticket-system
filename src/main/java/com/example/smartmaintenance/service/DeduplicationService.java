package com.example.smartmaintenance.service;

import com.example.smartmaintenance.dto.response.DedupStatsResponse;
import com.example.smartmaintenance.enums.EventType;
import com.example.smartmaintenance.repository.AlarmEventRepository;
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

    public DedupStatsResponse getStats() {
        long validEvents = alarmEventRepository.count();
        long duplicatedEvents = cacheService.getDuplicateEventCount();
        long totalProcessedEvents = validEvents + duplicatedEvents;
        double dedupRate = totalProcessedEvents == 0 ? 0.0 : (double) duplicatedEvents / totalProcessedEvents;
        return new DedupStatsResponse(totalProcessedEvents, validEvents, duplicatedEvents, dedupRate);
    }
}